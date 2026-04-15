from __future__ import annotations

import json
import logging
from typing import List

from langchain_core.messages import HumanMessage

from src.services.mws_llm_factory import build_mws_chat_llm
from src.settings.config import APISettings
from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)


async def node_plan(state: MigrationGraphState) -> dict:
    """Stage 2: Strategic Planning & Refactoring Map.

    Generates a structured migration plan based on the Java analysis results,
    including project structure, endpoint mapping, library choices, and
    migration strategy for data layer, business logic, and API layer.
    """
    structure = state.get("java_structure", {})
    contract = state.get("api_contract", [])
    dependency_graph = state.get("dependency_graph", {})

    if not contract:
        logger.warning("API contract is empty — cannot generate plan")
        return {
            "migration_plan": (
                "Error: API contract is empty. "
                "Analysis stage may have failed."
            ),
            "plan_steps": [],
            "status": "plan_error",
            "current_node": "plan",
        }

    # Build rich context for the LLM
    controllers_summary = []
    for ctrl in structure.get("controllers", []):
        ctrl_name = (
            ctrl.get("class_name", "Unknown")
            if isinstance(ctrl, dict) else str(ctrl)
        )
        methods = ctrl.get("methods", []) if isinstance(ctrl, dict) else []
        controllers_summary.append({
            "name": ctrl_name,
            "endpoints": [
                {
                    "handler": m.get("name", ""),
                    "http_method": m.get("http_method", ""),
                    "path": m.get("path", ""),
                    "request_body": m.get("request_body_type", ""),
                    "response_type": m.get("return_type", ""),
                }
                for m in methods if m.get("http_method")
            ],
        })

    dtos_summary = []
    for dto in structure.get("dtos", []):
        if isinstance(dto, dict):
            dtos_summary.append({
                "name": dto.get("class_name", ""),
                "fields": [
                    {
                        "name": f.get("name", ""),
                        "type": f.get("type", ""),
                        "annotations": f.get("annotations", []),
                    }
                    for f in dto.get("fields", [])
                ],
            })
        else:
            dtos_summary.append({"name": str(dto), "fields": []})

    services_summary = []
    for svc in structure.get("services", []):
        if isinstance(svc, dict):
            services_summary.append({
                "name": svc.get("class_name", ""),
                "methods": [
                    m.get("name", "") for m in svc.get("methods", [])
                ],
                "dependencies": [
                    d.get("type", "")
                    for d in svc.get("injected_dependencies", [])
                ],
            })

    context_str = json.dumps({
        "package": structure.get("package", "N/A"),
        "controllers": controllers_summary,
        "dtos": dtos_summary,
        "services": services_summary,
        "dependency_graph": dependency_graph,
        "api_contract": contract,
    }, indent=2, ensure_ascii=False)

    prompt_content = (
        "You are a Senior Go engineer performing a Java Spring Boot "
        "to Go migration.\n\n"
        f"## Java Project Analysis\n\n```json\n{context_str}\n```\n\n"
        "## Task\n\n"
        "Create a detailed migration plan with the following sections:\n\n"
        "### 1. Go Project Structure\n"
        "Define the Go module name, package layout (cmd/, internal/handlers/, "
        "internal/models/, internal/services/, internal/repository/), "
        "and file organization.\n\n"
        "### 2. Endpoint Mapping\n"
        "For each Java endpoint, specify the corresponding Go handler "
        "function name, the router path, and which Gin handler group "
        "it belongs to.\n\n"
        "### 3. Library Mapping\n"
        "Map Spring Boot libraries/patterns to Go equivalents:\n"
        "- Spring Web → Gin\n"
        "- Spring DI → Constructor injection via structs\n"
        "- JPA/Hibernate → GORM or sqlx\n"
        "- Jackson → encoding/json\n"
        "- Bean Validation → go-playground/validator\n"
        "- Spring Security → middleware\n"
        "- Logging → zerolog or slog\n\n"
        "### 4. Data Layer Strategy\n"
        "Describe how to convert Java DTOs/Entities to Go structs, "
        "including type mappings (Optional→pointer, List→slice, etc.) "
        "and annotation-to-tag conversions.\n\n"
        "### 5. Business Logic Strategy\n"
        "For each service class, describe which methods can be "
        "auto-migrated and which require manual intervention "
        "(mark with complexity: simple/medium/complex).\n\n"
        "### 6. Manual Refactoring Steps\n"
        "List specific areas that require human review or manual coding.\n\n"
        "### 7. Testing Strategy\n"
        "Describe the testing approach: unit tests, integration tests, "
        "contract comparison between Java and Go services.\n\n"
        "Respond in Markdown format."
    )

    settings = APISettings()
    llm = build_mws_chat_llm(settings)

    try:
        msg = await llm.ainvoke([HumanMessage(content=prompt_content)])
        text = (
            msg.content if isinstance(msg.content, str)
            else str(msg.content)
        )

        plan_steps = _extract_plan_steps(text)

        logger.info(
            f"Migration plan generated: "
            f"{len(text)} chars, {len(plan_steps)} steps"
        )

        return {
            "migration_plan": text,
            "plan_steps": plan_steps,
            "status": "planned",
            "current_node": "plan",
        }
    except Exception as e:
        logger.exception(f"Error generating migration plan: {e}")
        return {
            "migration_plan": f"Error generating plan: {str(e)}",
            "plan_steps": [],
            "status": "plan_error",
            "current_node": "plan",
        }


def _extract_plan_steps(markdown_text: str) -> List[str]:
    """Extract numbered section headers from the migration plan markdown."""
    steps: List[str] = []
    for line in markdown_text.split("\n"):
        stripped = line.strip()
        if stripped.startswith("#") and any(
            c.isdigit() for c in stripped[:10]
        ):
            step_text = stripped.lstrip("#").strip()
            if step_text:
                steps.append(step_text)
    return steps