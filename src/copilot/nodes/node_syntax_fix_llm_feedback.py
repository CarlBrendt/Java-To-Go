# src/copilot/nodes/syntax_fix_llm_feedback.py
from __future__ import annotations

import logging
import re

from langchain_core.messages import HumanMessage, SystemMessage

from src.copilot.graph_state import MigrationGraphState
from src.copilot.nodes.linter_node import _fix_unclosed_braces
from src.services.mws_llm_factory import build_mws_chat_llm
from src.settings.config import APISettings

settings = APISettings()
logger = logging.getLogger(__name__)

SYSTEM_PROMPT_FEEDBACK = """
You are a Go syntax correction expert.
Your task is to fix **ONLY** the syntax errors reported by the Go compiler.
Do **NOT** refactor, optimize, or change logic.
Do **NOT** add comments or new code.
Return the **complete corrected Go file content**, nothing else.

Compiler errors for this file:
{{ERRORS}}

Original code:
```go
{{CODE}}
```"""

async def node_syntax_fix_llm_feedback(state: MigrationGraphState) -> dict:
    """
    Исправляет синтаксис на основе ошибок от golangci-lint.
    Запускается итеративно после lint_check.
    """
    generated_code = dict(state.get("generated_go_code", {}))
    lint_issues = state.get("lint_issues", [])
    fixes = []

    if not lint_issues:
        return {
            "generated_go_code": generated_code,
            "llm_syntax_fixes": fixes,
            "lint_issues": [],
            "status": "llm_syntax_feedback_skipped_no_issues",
            "current_node": "syntax_fix_llm_feedback",
        }

    # Фильтруем только синтаксические ошибки
    syntax_issues = [
        issue for issue in lint_issues
        if "expected" in issue.get("message", "") or "found" in issue.get("message", "")
    ]

    if not syntax_issues:
        return {
            "generated_go_code": generated_code,
            "llm_syntax_fixes": fixes,
            "lint_issues": [],
            "status": "llm_syntax_feedback_no_syntax_errors",
            "current_node": "syntax_fix_llm_feedback",
        }

    try:
        llm = build_mws_chat_llm(settings, state.get("mws_model_name"))
    except Exception as e:
        logger.warning(f"LLM not available for syntax fix feedback: {e}")
        return {
            "generated_go_code": generated_code,
            "llm_syntax_fixes": fixes,
            "lint_issues": [],
            "status": "llm_syntax_feedback_failed",
            "current_node": "syntax_fix_llm_feedback",
        }

    # Группируем ошибки по файлу
    issues_by_file = {}
    for issue in syntax_issues:
        fname = issue["file"]
        if fname not in issues_by_file:
            issues_by_file[fname] = []
        issues_by_file[fname].append(f"{issue['line']}:{issue['column']} {issue['message']}")

    # Обрабатываем каждый файл с ошибками
    for filename, file_issues in issues_by_file.items():
        if not filename.endswith(".go") or filename not in generated_code:
            continue

        original = generated_code[filename].strip()
        errors_text = "\n".join(file_issues)

        try:
            prompt = SYSTEM_PROMPT_FEEDBACK.replace("{{ERRORS}}", errors_text).replace("{{CODE}}", original)
            messages = [
                SystemMessage(content="You are a Go syntax correction expert."),
                HumanMessage(content=prompt),
            ]

            response = await llm.ainvoke(messages)
            corrected = response.content.strip()

            # Извлекаем код из markdown
            if corrected.startswith("```go"):
                corrected = re.sub(r"^```go\n?", "", corrected)
                corrected = re.sub(r"\n?```.*$", "", corrected)

            corrected = _fix_unclosed_braces(corrected)

            if corrected != original:
                generated_code[filename] = corrected
                fixes.append(f"{filename}: fixed via lint feedback (LLM)")
                logger.info(f"LLM fixed syntax in {filename} using lint feedback")

        except Exception as e:
            logger.warning(f"LLM feedback fix failed for {filename}: {e}")
            continue

    return {
        "generated_go_code": generated_code,
        "llm_syntax_fixes": fixes,
        "lint_issues": [],
        "status": "llm_syntax_feedback_fixed" if fixes else "llm_syntax_feedback_clean",
        "current_node": "syntax_fix_llm_feedback",
    }
