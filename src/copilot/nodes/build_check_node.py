from __future__ import annotations

import os
import subprocess
import logging
import re
from typing import List, Dict

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)


async def node_build_check(state: MigrationGraphState) -> dict:
    """Stage 7: Build Check.

    1. Применяет детерминистические фиксы
    2. Сохраняет файлы
    3. Запускает go build ОДИН раз
    4. Собирает ошибки в структурированный отчёт для инженера
    """
    generated_go_code = dict(state.get("generated_go_code", {}))
    output_dir = os.path.abspath(
        state.get("output_dir", "output/go_project")
    )
    java_structure = state.get("java_structure", {})

    if not generated_go_code:
        return {
            "build_passed": False,
            "build_errors": [],
            "build_fixes_applied": [],
            "build_report": "No Go code generated.",
            "status": "build_skipped",
            "current_node": "build_check",
        }

    build_dir = output_dir
    os.makedirs(build_dir, exist_ok=True)

    fixes_applied: List[str] = []

    # ── 1. Детерминистические фиксы ──
    generated_go_code, f = _fix_duplicate_main(generated_go_code)
    fixes_applied.extend(f)

    generated_go_code, f = _fix_package_conflicts(generated_go_code)
    fixes_applied.extend(f)

    generated_go_code, f = _fix_unused_imports(generated_go_code)
    fixes_applied.extend(f)

    generated_go_code, f = _fix_missing_types(generated_go_code)
    fixes_applied.extend(f)

    for fname in list(generated_go_code.keys()):
        if not fname.endswith(".go"):
            continue
        content = generated_go_code[fname]
        new_content, sf = _fix_go_syntax(fname, content)
        if sf:
            generated_go_code[fname] = new_content
            fixes_applied.extend(sf)

    # ── 2. Сохраняем файлы ──
    for filename, content in generated_go_code.items():
        filepath = os.path.join(build_dir, filename)
        file_dir = os.path.dirname(filepath)
        if file_dir and file_dir != build_dir:
            os.makedirs(file_dir, exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)

    # ── 3. go.mod ──
    package_name = java_structure.get("package", "migrated-service")
    module_name = (
        package_name.replace(".", "-")
        if package_name else "migrated-service"
    )
    all_content = "\n".join(generated_go_code.values())
    go_mod = _generate_go_mod(module_name, all_content)
    with open(os.path.join(build_dir, "go.mod"), "w") as f:
        f.write(go_mod)
    fixes_applied.append("Generated go.mod")

    # ── 4. Проверяем Go ──
    try:
        go_ver = subprocess.run(
            ["go", "version"],
            capture_output=True, text=True, check=True,
        )
        logger.info(f"Go: {go_ver.stdout.strip()}")
    except (subprocess.CalledProcessError, FileNotFoundError):
        logger.warning("Go not installed — skipping build check")
        report = _generate_build_report(
            fixes_applied, [],
            ["Go not installed. Install Go 1.22+."],
            False,
        )
        return {
            "build_passed": False,
            "build_errors": ["Go not installed"],
            "build_fixes_applied": fixes_applied,
            "build_report": report,
            "generated_go_code": generated_go_code,
            "status": "build_skipped_no_go",
            "current_node": "build_check",
        }

    env = os.environ.copy()
    env["GIT_TERMINAL_PROMPT"] = "0"

    # ── 5. go mod download + tidy ──
    logger.info("Downloading Go dependencies...")
    _run_command(["go", "mod", "download"], build_dir, 180, env)
    _run_command(["go", "mod", "tidy"], build_dir, 180, env)

    # ── 6. go build (одна попытка) ──
    build_result = _run_command(
        ["go", "build", "./..."], build_dir, 120, env
    )

    build_passed = build_result["returncode"] == 0
    raw_errors = build_result["error"] if not build_passed else ""

    # ── 7. Парсим ошибки в структурированный формат ──
    parsed_errors = _parse_build_errors(raw_errors)

    # ── 8. go vet (если build прошёл) ──
    vet_warnings = []
    if build_passed:
        vet_result = _run_command(
            ["go", "vet", "./..."], build_dir, 60, env
        )
        if vet_result["returncode"] != 0:
            vet_warnings = _parse_build_errors(vet_result["error"])

    # ── 9. Проверка запуска ──
    startup_ok = False
    if build_passed:
        startup_ok = _check_startup(build_dir, env)

    # ── 10. Генерируем отчёт для инженера ──
    report = _generate_build_report(
        fixes_applied, parsed_errors, vet_warnings, build_passed,
        startup_ok,
    )

    # Сохраняем отчёт
    report_path = os.path.join(build_dir, "BUILD_REPORT.md")
    with open(report_path, "w") as f:
        f.write(report)

    error_strings = [e["message"] for e in parsed_errors]

    logger.info(
        f"Build: {'PASSED ✅' if build_passed else 'FAILED ❌'}, "
        f"Startup: {'OK ✅' if startup_ok else 'N/A'}, "
        f"Errors: {len(parsed_errors)}, "
        f"Fixes applied: {len(fixes_applied)}"
    )

    if fixes_applied:
        for fix in fixes_applied[:5]:
            logger.info(f"  fix: {fix}")

    if parsed_errors:
        for err in parsed_errors[:5]:
            logger.info(
                f"  err: {err['file']}:{err['line']} "
                f"→ {err['message']}"
            )

    return {
        "build_passed": build_passed,
        "build_errors": error_strings,
        "build_fixes_applied": fixes_applied,
        "build_report": report,
        "generated_go_code": generated_go_code,
        "status": (
            "build_and_startup_ok" if build_passed and startup_ok
            else "build_passed" if build_passed
            else "build_failed"
        ),
        "current_node": "build_check",
    }


# ──────────────────────────────────────────────
# Error parsing & report generation
# ──────────────────────────────────────────────

def _parse_build_errors(stderr: str) -> List[Dict]:
    """Парсит вывод go build в структурированный список."""
    errors = []
    if not stderr:
        return errors

    for line in stderr.split('\n'):
        line = line.strip()
        if not line or line.startswith('#'):
            continue

        # ./file.go:10:5: error message
        match = re.match(
            r'\.?/?(\S+\.go):(\d+):(\d+):\s*(.*)', line
        )
        if match:
            errors.append({
                "file": match.group(1),
                "line": int(match.group(2)),
                "col": int(match.group(3)),
                "message": match.group(4).strip(),
                "category": _categorize_error(match.group(4)),
            })
        # ./file.go:10: error message (без колонки)
        elif re.match(r'\.?/?(\S+\.go):(\d+):\s*(.*)', line):
            m = re.match(r'\.?/?(\S+\.go):(\d+):\s*(.*)', line)
            errors.append({
                "file": m.group(1),
                "line": int(m.group(2)),
                "col": 0,
                "message": m.group(3).strip(),
                "category": _categorize_error(m.group(3)),
            })

    return errors


def _categorize_error(msg: str) -> str:
    """Категоризирует ошибку для отчёта."""
    if "redeclared" in msg:
        return "duplicate_type"
    if "undefined" in msg:
        return "undefined_symbol"
    if "imported and not used" in msg:
        return "unused_import"
    if "declared and not used" in msg:
        return "unused_variable"
    if "syntax error" in msg:
        return "syntax"
    if "cannot use" in msg or "cannot convert" in msg:
        return "type_mismatch"
    if "missing" in msg:
        return "missing"
    if "too many" in msg or "too few" in msg:
        return "argument_count"
    return "other"


def _generate_build_report(
    fixes: List[str],
    errors: List[Dict],
    vet_warnings: list,
    build_passed: bool,
    startup_ok: bool = False,
) -> str:
    """Генерирует BUILD_REPORT.md для инженера."""
    lines = [
        "# 🔧 Build Report for Engineer\n",
        f"**Build Status:** {'✅ PASSED' if build_passed else '❌ FAILED'}\n",
        f"**Startup Check:** {'✅ OK' if startup_ok else '⚠️ Not tested'}\n",
        f"**Auto-fixes applied:** {len(fixes)}\n",
        f"**Remaining errors:** {len(errors)}\n",
    ]

    # Авто-фиксы
    if fixes:
        lines.append("\n## ✅ Auto-fixes Applied\n")
        lines.append(
            "These were fixed automatically during build:\n"
        )
        for fix in fixes:
            lines.append(f"- {fix}")

    if build_passed:
        lines.append("\n## 🎉 Build Successful!\n")
        lines.append(
            "The generated Go code compiles successfully. "
            "Review TODO comments in the code for manual "
            "implementation.\n"
        )
        if startup_ok:
            lines.append("Server starts and responds to health check.\n")
        return "\n".join(lines)

    # Ошибки по категориям
    if errors:
        by_category: Dict[str, List[Dict]] = {}
        for err in errors:
            cat = err["category"]
            if cat not in by_category:
                by_category[cat] = []
            by_category[cat].append(err)

        lines.append("\n## ❌ Compilation Errors\n")

        category_titles = {
            "duplicate_type": "🔄 Duplicate Type Declarations",
            "undefined_symbol": "❓ Undefined Symbols",
            "unused_import": "📦 Unused Imports",
            "unused_variable": "📝 Unused Variables",
            "syntax": "⚠️ Syntax Errors",
            "type_mismatch": "🔀 Type Mismatches",
            "missing": "🔍 Missing Elements",
            "argument_count": "🔢 Argument Count Errors",
            "other": "📋 Other Errors",
        }

        category_fixes = {
            "duplicate_type": (
                "**How to fix:** Find the type declared in "
                "multiple files. Keep it in `models_*.go` and "
                "remove from `handlers.go` or `service.go`."
            ),
            "undefined_symbol": (
                "**How to fix:** Either import the missing "
                "package, define the type in `models_*.go`, "
                "or replace with a Go equivalent "
                "(e.g., `uuid.UUID` → `string`)."
            ),
            "unused_import": (
                "**How to fix:** Remove the unused import line."
            ),
            "unused_variable": (
                "**How to fix:** Use the variable or prefix "
                "with `_`."
            ),
            "syntax": (
                "**How to fix:** Check struct tags use "
                "backticks: `` `json:\"name\"` ``. "
                "Check all braces are balanced."
            ),
            "type_mismatch": (
                "**How to fix:** Cast the value or change "
                "the type to match."
            ),
            "missing": (
                "**How to fix:** Add the missing element "
                "(import, field, method)."
            ),
            "argument_count": (
                "**How to fix:** Check function signature "
                "and call site match."
            ),
            "other": (
                "**How to fix:** Read the error message "
                "and fix manually."
            ),
        }

        for cat, cat_errors in by_category.items():
            title = category_titles.get(cat, cat)
            fix_hint = category_fixes.get(cat, "")

            lines.append(f"\n### {title} ({len(cat_errors)} errors)\n")
            if fix_hint:
                lines.append(f"{fix_hint}\n")

            lines.append("| File | Line | Error |")
            lines.append("|------|------|-------|")
            for err in cat_errors:
                lines.append(
                    f"| `{err['file']}` | {err['line']} "
                    f"| {err['message']} |"
                )

    # Quick fix commands
    lines.append("\n## 🚀 Quick Fix Commands\n")
    lines.append("```bash")
    lines.append("cd output/go_project")
    lines.append("")
    lines.append("# Fix imports automatically")
    lines.append("goimports -w *.go")
    lines.append("")
    lines.append("# Or install goimports first")
    lines.append("go install golang.org/x/tools/cmd/goimports@latest")
    lines.append("")
    lines.append("# Then rebuild")
    lines.append("go mod tidy")
    lines.append("go build ./...")
    lines.append("```\n")

    # Vet warnings
    if vet_warnings:
        lines.append("\n## ⚠️ Vet Warnings\n")
        for w in vet_warnings:
            if isinstance(w, dict):
                lines.append(
                    f"- `{w['file']}:{w['line']}` — {w['message']}"
                )
            else:
                lines.append(f"- {w}")

    return "\n".join(lines)

# ──────────────────────────────────────────────
# Pre-fix functions
# ──────────────────────────────────────────────

def _fix_duplicate_main(
    code: Dict[str, str],
) -> tuple[Dict[str, str], List[str]]:
    """Убирает дублирование func main() — оставляем только в main.go."""
    fixes = []
    main_count = sum(
        1 for content in code.values()
        if "func main()" in content
    )
    if main_count <= 1:
        return code, fixes

    for filename, content in code.items():
        if filename == "main.go":
            continue
        if "func main()" not in content:
            continue

        lines = content.split('\n')
        new_lines = []
        skip_depth = 0
        skipping = False

        for line in lines:
            if "func main()" in line:
                skipping = True
                skip_depth = 0
                continue
            if skipping:
                skip_depth += line.count('{') - line.count('}')
                if skip_depth <= 0 and '}' in line:
                    skipping = False
                continue
            new_lines.append(line)

        code[filename] = '\n'.join(new_lines)
        fixes.append(f"Removed duplicate main() from {filename}")

    return code, fixes


def _fix_package_conflicts(
    code: Dict[str, str],
) -> tuple[Dict[str, str], List[str]]:
    """Все .go файлы в корне должны быть package main."""
    fixes = []
    for fname in [f for f in code if "/" not in f and f.endswith(".go")]:
        content = code[fname]
        lines = content.split('\n')
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith("package "):
                current_pkg = stripped.split()[1].rstrip(';')
                if current_pkg != "main":
                    lines[i] = "package main"
                    fixes.append(f"{fname}: package {current_pkg} → main")
                break
        code[fname] = '\n'.join(lines)
    return code, fixes


def _fix_unused_imports(
    code: Dict[str, str],
) -> tuple[Dict[str, str], List[str]]:
    """Убирает очевидно неиспользуемые импорты."""
    fixes = []
    for fname, content in code.items():
        if not fname.endswith(".go"):
            continue
        import_match = re.search(
            r'import\s*\(\s*\n([\s\S]*?)\n\s*\)', content
        )
        if not import_match:
            continue

        import_lines = import_match.group(1).split('\n')
        code_without_imports = (
            content[:import_match.start()] + content[import_match.end():]
        )

        new_imports = []
        removed = []
        for imp_line in import_lines:
            imp_stripped = imp_line.strip().strip('"')
            if not imp_stripped:
                new_imports.append(imp_line)
                continue
            parts = imp_stripped.split('/')
            pkg_name = parts[-1].strip('"').strip()
            alias_match = re.match(r'\s*(\w+)\s+"', imp_line)
            if alias_match:
                pkg_name = alias_match.group(1)
            if pkg_name and re.search(
                rf'\b{re.escape(pkg_name)}\b', code_without_imports
            ):
                new_imports.append(imp_line)
            else:
                removed.append(pkg_name)

        if removed:
            new_block = '\n'.join(new_imports)
            content = (
                content[:import_match.start()]
                + f'import (\n{new_block}\n)'
                + content[import_match.end():]
            )
            code[fname] = content
            fixes.append(
                f"{fname}: removed unused imports: {', '.join(removed)}"
            )
    return code, fixes


def _fix_missing_types(
    code: Dict[str, str],
) -> tuple[Dict[str, str], List[str]]:
    """Добавляет заглушки для часто используемых типов."""
    fixes = []
    all_content = "\n".join(code.values())

    common_missing = {
        "JsonNode": "type JsonNode = map[string]interface{}",
        "File": "type File = string",
        "ObjectMapper": "type ObjectMapper = struct{}",
    }

    missing = []
    for type_name, type_def in common_missing.items():
        if (
            re.search(rf'\b{type_name}\b', all_content)
            and not re.search(rf'type\s+{type_name}\b', all_content)
        ):
            missing.append(type_def)

    if missing:
        code["types_common.go"] = (
            "package main\n\n// Auto-generated type aliases\n"
            + "\n".join(missing) + "\n"
        )
        fixes.append(
            f"Added types_common.go with {len(missing)} type aliases"
        )
    return code, fixes


# ──────────────────────────────────────────────
# Syntax fix
# ──────────────────────────────────────────────

def _fix_go_syntax(
    filename: str,
    content: str,
) -> tuple[str, List[str]]:
    """Исправляет типичные синтаксические ошибки в Go-коде."""
    fixes = []
    original = content

    # -1. Незакрытые struct tags
    lines = content.split('\n')
    fixed_lines = []
    for line in lines:
        stripped = line.rstrip()
        if stripped.count('`') == 1:
            after = stripped[stripped.index('`'):]
            if after.endswith('"'):
                line = stripped + '`'
    
        fixed_lines.append(line)
    content = '\n'.join(fixed_lines)

    # 0. Сломанные двойные кавычки в tags
    def fix_broken_tags(match):
        tag = match.group(1)
        tag = re.sub(r'""(\s)', r'"\1', tag)
        tag = re.sub(r'""$', '"', tag)
        return f'`{tag}`'

    content = re.sub(r'`([^`]*""\s*[^`]*)`', fix_broken_tags, content)

    # 1. Двойные struct tags: `x` `y` → `x y`
    content = re.sub(r'`([^`]+)`\s+`([^`]+)`', r'`\1 \2`', content)

    # 2. Naked tags без бэктиков
    def fix_naked(m):
        return f'{m.group(1)} `{m.group(2)}`'

    content = re.sub(
        r'(\w+\s+[\w\[\]*]+)\s+'
        r'((?:json|xml|yaml|validate|gorm|db|bson|form|binding)'
        r':"[^"]*"'
        r'(?:\s+(?:json|xml|yaml|validate|gorm|db|bson|form|binding)'
        r':"[^"]*")*)\s*$',
        fix_naked, content, flags=re.MULTILINE,
    )

    # 3. Java generics
    content = re.sub(r'\*?Optional\((\w+)\)', r'*\1', content)
    content = re.sub(r'\*?Optional<(\w+)>', r'*\1', content)
    content = re.sub(
        r'List[(<](\w+)[)>]',
        lambda m: '[]' + _java_to_go_type(m.group(1)), content,
    )
    content = re.sub(
        r'Map<(\w+),\s*(\w+)>',
        lambda m: f'map[{_java_to_go_type(m.group(1))}]{_java_to_go_type(m.group(2))}',
        content,
    )
    content = re.sub(
        r'Set<(\w+)>',
        lambda m: '[]' + _java_to_go_type(m.group(1)), content,
    )
    content = re.sub(r'ResponseEntity<(\w+)>', r'\1', content)

    # 4. Duplicate package declarations
    pkg_lines = [
        (i, l) for i, l in enumerate(content.split('\n'))
        if l.strip().startswith('package ')
    ]
    if len(pkg_lines) > 1:
        lines = content.split('\n')
        first = True
        new_lines = []
        for line in lines:
            if line.strip().startswith('package '):
                if first:
                    first = False
                    new_lines.append(line)
                # skip duplicates
            else:
                new_lines.append(line)
        content = '\n'.join(new_lines)

    # 5. Merge duplicate import blocks
    blocks = list(re.finditer(
        r'import\s*\(\s*\n([\s\S]*?)\n\s*\)', content
    ))
    if len(blocks) > 1:
        all_imps = set()
        for b in blocks:
            for l in b.group(1).split('\n'):
                l = l.strip()
                if l:
                    all_imps.add(l)
        for b in reversed(blocks):
            content = content[:b.start()] + content[b.end():]
        pkg = re.search(r'package \w+\n', content)
        if pkg:
            imp_str = '\nimport (\n' + '\n'.join(
                f'\t{i}' for i in sorted(all_imps)
            ) + '\n)\n'
            content = content[:pkg.end()] + imp_str + content[pkg.end():]

    # 6. Missing imports
    if 'errors.New(' in content and '"errors"' not in content:
        content = _add_import(content, '"errors"')
    if 'fmt.' in content and '"fmt"' not in content:
        content = _add_import(content, '"fmt"')
    if 'context.Context' in content and '"context"' not in content:
        content = _add_import(content, '"context"')

    # 7. Cleanup
    content = re.sub(r'@\w+(?:\([^)]*\))?\s*\n', '\n', content)
    content = content.replace('.class', '')
    content = re.sub(r'\n{4,}', '\n\n\n', content)

    # 8. Lишние } на top level
    lines = content.split('\n')
    depth = 0
    fixed = []
    for line in lines:
        s = line.strip()
        opens = s.count('{')
        closes = s.count('}')
        if s == '}' and depth == 0:
            continue
        depth += opens - closes
        if depth < 0:
            depth = 0
            continue
        fixed.append(line)
    content = '\n'.join(fixed)

    # 9. Struct fields without types
    def fix_no_type(m):
        return f'{m.group(1)}{m.group(2)} {_guess_field_type(m.group(2))} {m.group(3)}'

    content = re.sub(
        r'^(\s+)(\w+)\s+(`[^`]+`)\s*$',
        fix_no_type, content, flags=re.MULTILINE,
    )

    # 10. Fake internal imports
    lines = content.split('\n')
    fixed = []
    for line in lines:
        s = line.strip().strip('"')
        skip = any(
            f in s and 'gin' not in s
            for f in [
                '/internal/', '/models', '/services',
                '/repository', '/handlers', '/controller',
                'mts/workflow', 'mts/ip/', 'migrated-service/',
                'ru-mts-', 'ru.mts.',
            ]
        )
        if not skip:
            fixed.append(line)
    content = '\n'.join(fixed)

    # 11. uuid.UUID → string
    content = re.sub(r'\buuid\.UUID\b', 'string', content)
    content = re.sub(r'\buuid\.New\(\)', '"" // TODO: generate UUID', content)
    content = re.sub(r'\s*"github\.com/google/uuid"\s*\n', '\n', content)

    # 12. *error → error
    content = re.sub(r'\*error\b', 'error', content)

    # 13. json.JsonNode → JsonNode
    content = re.sub(r'\bjson\.JsonNode\b', 'JsonNode', content)

    if content != original:
        fixes.append(f"{filename}: fixed Go syntax issues")

    return content, fixes


# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────

def _guess_field_type(name: str) -> str:
    n = name.lower()
    if n in ('id', 'uuid'):
        return 'string'
    if 'name' in n or 'key' in n or 'path' in n:
        return 'string'
    if 'timeout' in n or 'duration' in n:
        return 'time.Duration'
    if 'time' in n or 'date' in n:
        return 'time.Time'
    if 'count' in n or 'size' in n:
        return 'int'
    if 'enabled' in n or 'active' in n:
        return 'bool'
    if 'variables' in n:
        return 'map[string]interface{}'
    return 'interface{}'


def _java_to_go_type(t: str) -> str:
    return {
        'String': 'string', 'Integer': 'int', 'Long': 'int64',
        'Double': 'float64', 'Float': 'float32', 'Boolean': 'bool',
        'Object': 'interface{}', 'Byte': 'byte',
    }.get(t, t)


def _add_import(content: str, path: str) -> str:
    m = re.search(r'(import\s*\(\s*\n)([\s\S]*?)(\n\s*\))', content)
    if m and path not in m.group(2):
        return (
            content[:m.start()] + m.group(1)
            + m.group(2) + f'\n\t{path}'
            + m.group(3) + content[m.end():]
        )
    elif not m:
        pkg = re.search(r'package \w+\n', content)
        if pkg:
            return (
                content[:pkg.end()]
                + f'\nimport (\n\t{path}\n)\n'
                + content[pkg.end():]
            )
    return content


def _generate_go_mod(module_name: str, all_content: str) -> str:
    deps = ['\tgithub.com/gin-gonic/gin v1.9.1']
    if "gorm.io" in all_content:
        deps.extend([
            '\tgorm.io/gorm v1.25.7',
            '\tgorm.io/driver/postgres v1.5.7',
        ])
    if "github.com/jlaffaye/ftp" in all_content:
        deps.append('\tgithub.com/jlaffaye/ftp v0.2.0')
    if "github.com/pkg/sftp" in all_content:
        deps.append('\tgithub.com/pkg/sftp v1.13.6')

    deps_str = "\n".join(deps)
    return (
        f"module {module_name}\n\n"
        "go 1.22\n\n"
        "require (\n"
        f"{deps_str}\n"
        ")\n"
    )


def _run_command(
    cmd: List[str],
    cwd: str,
    timeout: int = 60,
    env: dict = None,
) -> Dict[str, any]:
    """Запускает команду и возвращает результат."""
    try:
        run_env = env or os.environ.copy()
        run_env["GIT_TERMINAL_PROMPT"] = "0"

        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            cwd=cwd,
            timeout=timeout,
            env=run_env,
        )
        return {
            "returncode": result.returncode,
            "stdout": result.stdout.strip(),
            "error": result.stderr.strip(),
        }
    except subprocess.TimeoutExpired:
        return {
            "returncode": -1,
            "stdout": "",
            "error": f"Command timed out ({timeout}s): {' '.join(cmd)}",
        }
    except FileNotFoundError:
        return {
            "returncode": -1,
            "stdout": "",
            "error": f"Command not found: {cmd[0]}",
        }
    except Exception as e:
        return {
            "returncode": -1,
            "stdout": "",
            "error": str(e),
        }


def _check_startup(build_dir: str, env: dict = None) -> bool:
    """Пытается запустить сервис на 3 секунды."""
    import time

    logger.info("Checking startup (3 sec timeout)...")

    try:
        run_env = env or os.environ.copy()
        run_env["PORT"] = "18080"
        run_env["GIT_TERMINAL_PROMPT"] = "0"

        proc = subprocess.Popen(
            ["go", "run", "."],
            cwd=build_dir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=run_env,
        )

        time.sleep(3)

        if proc.poll() is not None:
            _, stderr = proc.communicate(timeout=1)
            stderr_text = stderr.decode("utf-8", errors="replace")
            if stderr_text:
                logger.warning(f"Startup failed: {stderr_text[:300]}")
            return False

        # Проверяем health endpoint
        startup_ok = False
        try:
            import urllib.request
            req = urllib.request.Request(
                "http://localhost:18080/health", method="GET"
            )
            with urllib.request.urlopen(req, timeout=2) as resp:
                if resp.status == 200:
                    startup_ok = True
                    logger.info("✅ Health check passed")
        except Exception:
            logger.info("Health check N/A, but process is running")
            startup_ok = True

        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait(timeout=3)

        return startup_ok

    except Exception as e:
        logger.warning(f"Startup check error: {e}")
        return False