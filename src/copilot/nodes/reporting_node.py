"""
Отчет будет содержать теперь:

Consolidation fixes — что автоматически починено
Stubs summary — сколько заглушек, компактно
Skipped components — что осознанно не мигрировано
AI Usage — точно где используется LLM, а где шаблоны
Приоритизированные action items — 🔴🟡🔵⚪
Next Steps — разделены на immediate/short-term/medium-term
"""
from __future__ import annotations

import os
import re
import json
import logging
from datetime import datetime
from typing import Dict, Any, List

from src.copilot.graph_state import MigrationGraphState

logger = logging.getLogger(__name__)


async def node_reporting_packaging(state: MigrationGraphState) -> dict:
    """Stage 7: Reporting & Packaging."""
    generated_go_code = state.get("generated_go_code", {})
    verification_errors = state.get("verification_errors", [])
    manual_fixes = state.get("manual_fixes", [])
    java_structure = state.get("java_structure", {})
    api_contract = state.get("api_contract", [])
    migration_plan = state.get("migration_plan", "")
    output_dir = state.get("output_dir", "output/go_project")
    consolidation_fixes = state.get("consolidation_fixes", [])

    # ── 1. Сохранение кода ──
    os.makedirs(output_dir, exist_ok=True)

    saved_files: List[str] = []
    for filename, content in generated_go_code.items():
        filepath = os.path.join(output_dir, filename)
        file_dir = os.path.dirname(filepath)
        if file_dir and file_dir != output_dir:
            os.makedirs(file_dir, exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        saved_files.append(filename)

    # ── 2. Анализ TODO-заглушек ──
    todo_details: List[str] = []
    for filename, content in generated_go_code.items():
        for i, line in enumerate(content.split('\n'), 1):
            if "TODO" in line:
                todo_details.append(
                    f"- `{filename}:{i}`: {line.strip()}"
                )

    # ── 3. Анализ покрытия эндпоинтов ──
    all_go_content = "\n".join(generated_go_code.values())

    api_endpoints = [
        e for e in api_contract
        if not e.get("is_exception_handler")
    ]

    endpoints_migrated: List[Dict[str, str]] = []
    endpoints_missing: List[Dict[str, str]] = []

    for endpoint in api_endpoints:
        path = endpoint.get("path", "")
        method = endpoint.get("method", "").upper()
        handler = endpoint.get("handler_name", "")

        if not path or not method:
            continue

        search_path = path.replace("{", ":").rstrip("}")

        found = (
            (path in all_go_content or search_path in all_go_content)
            and method.lower() in all_go_content.lower()
        )

        entry = {
            "method": method,
            "path": path,
            "handler": handler,
            "class": endpoint.get("class_name", ""),
        }

        if found:
            endpoints_migrated.append(entry)
        else:
            endpoints_missing.append(entry)

    # ── 4. Анализ stubs ──
    stubs_summary = _analyze_stubs(generated_go_code)

    # ── 5. Анализ пропущенных компонентов ──
    skipped_components = _analyze_skipped_components(java_structure)

    # ── 6. Объединяем manual fixes ──
    all_manual_items: List[str] = []

    # Критичные — сначала
    for ep in endpoints_missing:
        all_manual_items.append(
            f"🔴 Реализовать эндпоинт `{ep['method']} {ep['path']}` "
            f"(Java: `{ep['handler']}` в `{ep['class']}`)"
        )

    # Stubs — одной строкой
    if stubs_summary["stub_types"] > 0:
        all_manual_items.append(
            f"🟡 `stubs_generated.go`: {stubs_summary['stub_types']} "
            f"типов-заглушек и {stubs_summary['stub_constructors']} "
            f"конструкторов — заменить реальными реализациями"
        )

    # TODO в handlers
    handler_todos = [
        td for td in todo_details
        if "handlers.go" in td and "implement business logic" in td
    ]
    if handler_todos:
        all_manual_items.append(
            f"🟡 `handlers.go`: {len(handler_todos)} обработчиков "
            f"с заглушками — реализовать бизнес-логику"
        )

    # TODO в сервисах
    service_todos = [
        td for td in todo_details
        if "service" in td.lower() and "TODO" in td
        and "stubs" not in td and "handlers" not in td
    ]
    if service_todos:
        all_manual_items.append(
            f"🟡 Сервисы: {len(service_todos)} TODO-комментариев "
            f"в бизнес-логике"
        )

    # Остальные manual fixes
    for fix in manual_fixes:
        if "go.mod" in fix:
            continue  # go.mod генерируется автоматически
        all_manual_items.append(f"🔵 {fix}")

    # Пропущенные компоненты
    for comp in skipped_components:
        all_manual_items.append(f"⚪ {comp}")

    # Дедупликация
    seen = set()
    unique_items: List[str] = []
    for item in all_manual_items:
        normalized = item.strip().lstrip("- 🔴🟡🔵⚪")
        if normalized not in seen:
            seen.add(normalized)
            unique_items.append(item)
    all_manual_items = unique_items

    # ── 7. Маппинг библиотек ──
    library_mapping = _build_library_mapping(all_go_content)

    # ── 8. Статистика ──
    total_endpoints = len(api_endpoints)
    migrated_count = len(endpoints_migrated)
    missing_count = len(endpoints_missing)
    coverage_pct = (
        round(migrated_count / total_endpoints * 100, 1)
        if total_endpoints > 0 else 0
    )

    verification_status = (
        "✅ PASSED" if not verification_errors else "❌ FAILED"
    )

    file_categories = {}
    for filename in generated_go_code:
        cat = _classify_file(filename)
        file_categories[cat] = file_categories.get(cat, 0) + 1

    # ── 9. JSON-отчёт ──
    report_json = {
        "migration_date": datetime.now().isoformat(),
        "status": (
            "success" if not verification_errors
            else "partial_success"
        ),
        "java_source": {
            "package": java_structure.get("package", ""),
            "controllers_count": len(
                java_structure.get("controllers", [])
            ),
            "services_count": len(
                java_structure.get("services", [])
            ),
            "dto_count": len(
                java_structure.get("dtos", [])
            ),
            "repositories_count": len(
                java_structure.get("repositories", [])
            ),
            "exception_handlers_count": len(
                java_structure.get("exception_handlers", [])
            ),
            "feign_clients_count": len(
                java_structure.get("feign_clients", [])
            ),
            "total_endpoints": total_endpoints,
        },
        "go_generated": {
            "files_count": len(generated_go_code),
            "files": list(generated_go_code.keys()),
            "categories": file_categories,
        },
        "coverage": {
            "endpoints_migrated": migrated_count,
            "endpoints_missing": missing_count,
            "coverage_percent": coverage_pct,
        },
        "build": {
            "passed": state.get("build_passed", False),
            "startup_ok": "build_and_startup_ok" == state.get(
                "status", ""
            ),
            "errors": state.get("build_errors", []),
            "auto_fixes": state.get("build_fixes_applied", []),
        },
        "consolidation": {
            "fixes_applied": len(consolidation_fixes),
            "details": consolidation_fixes[:20],
        },
        "verification": {
            "status": (
                "passed" if not verification_errors else "failed"
            ),
            "errors": verification_errors,
        },
        "stubs": stubs_summary,
        "skipped_components": skipped_components,
        "manual_intervention": {
            "total_items": len(all_manual_items),
            "items": all_manual_items,
        },
        "endpoints_migrated": endpoints_migrated,
        "endpoints_missing": endpoints_missing,
    }

    report_path = os.path.join(output_dir, "report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report_json, f, indent=2, ensure_ascii=False)

    # ── 10. Markdown-отчёт ──
    md = _build_markdown_report(
        state=state,
        report_json=report_json,
        java_structure=java_structure,
        generated_go_code=generated_go_code,
        endpoints_migrated=endpoints_migrated,
        endpoints_missing=endpoints_missing,
        all_manual_items=all_manual_items,
        stubs_summary=stubs_summary,
        skipped_components=skipped_components,
        consolidation_fixes=consolidation_fixes,
        todo_details=todo_details,
        library_mapping=library_mapping,
        migration_plan=migration_plan,
        total_endpoints=total_endpoints,
        migrated_count=migrated_count,
        missing_count=missing_count,
        coverage_pct=coverage_pct,
        verification_status=verification_status,
        verification_errors=verification_errors,
    )

    md_report_path = os.path.join(output_dir, "report.md")
    with open(md_report_path, "w", encoding="utf-8") as f:
        f.write(md)

    logger.info(
        f"Report generated: {md_report_path} "
        f"({migrated_count}/{total_endpoints} endpoints, "
        f"{len(all_manual_items)} manual fixes)"
    )

    # ── 11. go.mod ──
    _write_go_mod(output_dir, java_structure, generated_go_code)

    # ── 12. Dockerfile ──
    _write_dockerfile(output_dir)

    # ── 13. README ──
    _write_readme(output_dir, java_structure)

    return {
        "output_dir": output_dir,
        "status": "reporting_packaging_complete",
        "current_node": "reporting_packaging",
        "report_generated": True,
    }


# ═══════════════════════════════════════════════════
# Анализ stubs
# ═══════════════════════════════════════════════════

def _analyze_stubs(
    generated_go_code: Dict[str, str],
) -> Dict[str, Any]:
    """Анализирует stubs_generated.go и считает заглушки."""
    stubs_content = generated_go_code.get("stubs_generated.go", "")
    if not stubs_content:
        return {
            "stub_types": 0,
            "stub_constructors": 0,
            "stub_type_names": [],
            "stub_constructor_names": [],
        }

    stub_types = re.findall(
        r'type\s+(\w+)\s+struct\{\}', stubs_content
    )
    stub_constructors = re.findall(
        r'func\s+(New\w+)\s*\(', stubs_content
    )

    return {
        "stub_types": len(stub_types),
        "stub_constructors": len(stub_constructors),
        "stub_type_names": stub_types,
        "stub_constructor_names": stub_constructors,
    }


# ═══════════════════════════════════════════════════
# Анализ пропущенных компонентов
# ═══════════════════════════════════════════════════

def _analyze_skipped_components(
    java_structure: Dict[str, Any],
) -> List[str]:
    """Определяет что осознанно НЕ мигрировано."""
    skipped = []

    # Feign Clients → нужна HTTP-клиентская реализация
    feign_clients = java_structure.get("feign_clients", [])
    if feign_clients:
        names = [
            c.get("class_name", "?") for c in feign_clients[:5]
        ]
        skipped.append(
            f"Feign Clients ({len(feign_clients)} шт: "
            f"{', '.join(names)}) — нужна реализация HTTP-клиентов "
            f"(net/http или go-resty)"
        )

    # @Configuration классы — Spring-специфичные
    configs = [
        c for c in java_structure.get("services", [])
        if any(
            "@Configuration" in str(a)
            for a in c.get("annotations", [])
        )
    ]
    if configs:
        names = [c.get("class_name", "?") for c in configs[:5]]
        skipped.append(
            f"@Configuration классы ({len(configs)} шт: "
            f"{', '.join(names)}) — Spring-специфичные, "
            f"заменить на Go config/env"
        )

    # Repositories
    repos = java_structure.get("repositories", [])
    if repos:
        names = [r.get("class_name", "?") for r in repos[:5]]
        skipped.append(
            f"Repositories ({len(repos)} шт: "
            f"{', '.join(names)}) — реализовать через GORM/sqlx"
        )

    # Exception handlers
    exc_handlers = java_structure.get("exception_handlers", [])
    if exc_handlers:
        names = [e.get("class_name", "?") for e in exc_handlers[:5]]
        skipped.append(
            f"Exception Handlers ({len(exc_handlers)} шт: "
            f"{', '.join(names)}) — реализовать как Gin middleware"
        )

    return skipped


# ═══════════════════════════════════════════════════
# Маппинг библиотек (динамический)
# ═══════════════════════════════════════════════════

def _build_library_mapping(
    all_go_content: str,
) -> List[tuple]:
    """Строит маппинг библиотек на основе реально используемых."""
    mapping = [
        ("Spring Boot (@Controller, @Service)",
         "Go Structs + Methods"),
        ("Spring MVC (@GetMapping, @PostMapping)",
         "Gin Router"),
        ("Jackson (JSON)",
         "encoding/json + struct tags"),
        ("Lombok (@Data, @Builder)",
         "Go Structs (explicit fields)"),
    ]

    if "gorm.io" in all_go_content:
        mapping.append(
            ("Hibernate / JPA", "GORM")
        )
    else:
        mapping.append(
            ("Hibernate / JPA", "GORM / sqlx (не используется)")
        )

    if "gin.HandlerFunc" in all_go_content:
        mapping.append(
            ("Spring Security", "Gin Middleware")
        )

    if "validator" in all_go_content:
        mapping.append(
            ("Bean Validation (@NotNull)",
             "go-playground/validator")
        )

    if "zerolog" in all_go_content:
        mapping.append(("SLF4J / Logback", "zerolog"))
    else:
        mapping.append(("SLF4J / Logback", "log/slog (stdlib)"))

    if "ftp." in all_go_content:
        mapping.append(
            ("Apache Commons Net (FTP)",
             "github.com/jlaffaye/ftp")
        )

    if "sftp." in all_go_content:
        mapping.append(
            ("SSHJ / JSch (SFTP)", "github.com/pkg/sftp")
        )

    mapping.append(
        ("Spring @Transactional", "Manual DB transactions")
    )

    return mapping


# ═══════════════════════════════════════════════════
# Markdown отчёт
# ═══════════════════════════════════════════════════

def _build_markdown_report(
    state: Dict,
    report_json: Dict,
    java_structure: Dict,
    generated_go_code: Dict[str, str],
    endpoints_migrated: List[Dict],
    endpoints_missing: List[Dict],
    all_manual_items: List[str],
    stubs_summary: Dict,
    skipped_components: List[str],
    consolidation_fixes: List[str],
    todo_details: List[str],
    library_mapping: List[tuple],
    migration_plan: str,
    total_endpoints: int,
    migrated_count: int,
    missing_count: int,
    coverage_pct: float,
    verification_status: str,
    verification_errors: List[str],
) -> str:
    """Строит полный Markdown-отчёт."""
    md: List[str] = []

    build_passed = state.get("build_passed", False)
    startup_ok = state.get("status") == "build_and_startup_ok"
    build_errors = state.get("build_errors", [])
    build_fixes = state.get("build_fixes_applied", [])

    # ── Header ──
    md.append("# 🚀 Java → Go Migration Report\n")
    md.append(
        f"**Generated:** "
        f"{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  "
    )
    md.append(
        f"**Package:** "
        f"`{java_structure.get('package', 'unknown')}`\n"
    )

    # ── Executive Summary ──
    md.append("## 📊 Executive Summary\n")
    md.append("| Metric | Value |")
    md.append("|--------|-------|")

    status_emoji = "✅ SUCCESS" if build_passed else "⚠️ PARTIAL"
    md.append(f"| Status | **{status_emoji}** |")
    md.append(
        f"| Java Controllers | "
        f"{report_json['java_source']['controllers_count']} |"
    )
    md.append(
        f"| Java Services | "
        f"{report_json['java_source']['services_count']} |"
    )
    md.append(
        f"| Java DTOs | "
        f"{report_json['java_source']['dto_count']} |"
    )
    md.append(
        f"| Feign Clients | "
        f"{report_json['java_source'].get('feign_clients_count', 0)} |"
    )
    md.append(f"| Total Endpoints | {total_endpoints} |")
    md.append(f"| Endpoints Migrated | {migrated_count} |")
    md.append(f"| Coverage | **{coverage_pct}%** |")
    md.append(
        f"| Go Files Generated | {len(generated_go_code)} |"
    )
    md.append(f"| Verification | {verification_status} |")
    md.append(
        f"| Build | "
        f"{'✅ PASSED' if build_passed else '❌ FAILED'} |"
    )
    md.append(
        f"| Startup | "
        f"{'✅ OK' if startup_ok else '⚠️ Not verified'} |"
    )
    md.append(
        f"| Stub Types | {stubs_summary['stub_types']} |"
    )
    md.append(
        f"| Manual Items | {len(all_manual_items)} |"
    )
    md.append("")

    # ── Build & Startup ──
    md.append("## 🔧 Build & Startup\n")
    md.append(
        f"| Check | Result |"
    )
    md.append("|-------|--------|")
    md.append(
        f"| Compilation (`go build ./...`) | "
        f"{'✅ PASSED' if build_passed else '❌ FAILED'} |"
    )
    md.append(
        f"| Startup (port 8080) | "
        f"{'✅ OK' if startup_ok else '⚠️ Not verified'} |"
    )
    md.append(
        f"| Health Check (`/health`) | "
        f"{'✅ OK' if startup_ok else '⚠️ Not verified'} |"
    )
    md.append(
        f"| Auto-fixes applied | {len(build_fixes)} |"
    )
    md.append(
        f"| Remaining errors | {len(build_errors)} |"
    )
    md.append("")

    if build_fixes:
        md.append("### Auto-fixes Applied\n")
        for fix in build_fixes:
            md.append(f"- ✅ {fix}")
        md.append("")

    if build_errors:
        md.append("### Remaining Errors\n")
        md.append(
            "> See `BUILD_REPORT.md` for detailed fix instructions.\n"
        )
        for i, err in enumerate(build_errors[:10], 1):
            md.append(f"{i}. `{err}`")
        if len(build_errors) > 10:
            md.append(
                f"\n... and {len(build_errors) - 10} more errors"
            )
        md.append("")

    # ── Consolidation (что автоматически починено) ──
    if consolidation_fixes:
        md.append("## 🔄 Automatic Code Consolidation\n")
        md.append(
            f"**{len(consolidation_fixes)} automatic fixes** "
            f"applied to generated code:\n"
        )

        # Группируем по категории
        fix_categories = {}
        for fix in consolidation_fixes:
            if "package" in fix.lower():
                cat = "Package unification"
            elif "duplicate type" in fix.lower():
                cat = "Duplicate types removed"
            elif "duplicate func" in fix.lower() or "duplicate method" in fix.lower():
                cat = "Duplicate functions removed"
            elif "fake import" in fix.lower() or "rebuilt import" in fix.lower():
                cat = "Import cleanup"
            elif "missing comma" in fix.lower():
                cat = "Syntax fixes"
            elif "stub" in fix.lower():
                cat = "Stubs generated"
            elif "prefix" in fix.lower():
                cat = "Package prefix cleanup"
            elif "base type" in fix.lower():
                cat = "Base type aliases"
            else:
                cat = "Other cleanup"

            if cat not in fix_categories:
                fix_categories[cat] = 0
            fix_categories[cat] += 1

        for cat, count in sorted(
            fix_categories.items(), key=lambda x: -x[1]
        ):
            md.append(f"- {cat}: **{count}** fixes")
        md.append("")

        # Детали (свёрнуто)
        md.append("<details>")
        md.append("<summary>Show all consolidation fixes</summary>\n")
        for fix in consolidation_fixes:
            md.append(f"- {fix}")
        md.append("\n</details>\n")

    # ── Endpoints ──
    md.append("## ✅ Migrated Endpoints\n")
    if endpoints_migrated:
        md.append("| Method | Path | Handler | Java Class |")
        md.append("|--------|------|---------|------------|")
        seen = set()
        for ep in endpoints_migrated:
            key = f"{ep['method']}_{ep['path']}"
            if key in seen:
                continue
            seen.add(key)
            md.append(
                f"| `{ep['method']}` | `{ep['path']}` "
                f"| `{ep['handler']}` | `{ep['class']}` |"
            )
    else:
        md.append("⚠️ No endpoints verified.\n")
    md.append("")

    if endpoints_missing:
        md.append("## ❌ Missing Endpoints\n")
        md.append("| Method | Path | Handler | Java Class |")
        md.append("|--------|------|---------|------------|")
        for ep in endpoints_missing:
            md.append(
                f"| `{ep['method']}` | `{ep['path']}` "
                f"| `{ep['handler']}` | `{ep['class']}` |"
            )
        md.append("")
        md.append("> These need manual implementation.\n")

    # ── Generated Files ──
    md.append("## 📦 Generated Files\n")
    md.append("| File | Size | Purpose | Needs Review |")
    md.append("|------|------|---------|-------------|")
    for filename, content in generated_go_code.items():
        purpose = _classify_file(filename)
        has_todo = "TODO" in content
        review = "⚠️ Has TODOs" if has_todo else "✅ Ready"
        if filename == "stubs_generated.go":
            review = "🔴 All stubs"
        md.append(
            f"| `{filename}` | {len(content)} chars "
            f"| {purpose} | {review} |"
        )
    md.append("")

    # ── Stubs Summary ──
    if stubs_summary["stub_types"] > 0:
        md.append("## 🧩 Stub Types (need implementation)\n")
        md.append(
            f"**{stubs_summary['stub_types']} types** and "
            f"**{stubs_summary['stub_constructors']} constructors** "
            f"are empty stubs in `stubs_generated.go`.\n"
        )
        md.append(
            "These compile but have no real logic. "
            "Replace with actual implementations:\n"
        )

        # Группируем по 3 в строку для компактности
        names = stubs_summary.get("stub_type_names", [])
        for i in range(0, len(names), 4):
            chunk = names[i:i + 4]
            md.append("- " + ", ".join(f"`{n}`" for n in chunk))
        md.append("")

    # ── Skipped Components ──
    if skipped_components:
        md.append("## ⏭️ Not Migrated (intentionally)\n")
        md.append(
            "These Java components were **intentionally skipped** "
            "and need manual implementation:\n"
        )
        for comp in skipped_components:
            md.append(f"- {comp}")
        md.append("")

    # ── Manual Intervention ──
    md.append("## ⚠️ Action Items for Engineer\n")
    if all_manual_items:
        md.append(
            f"**{len(all_manual_items)} items** need attention:\n"
        )

        # Разделяем по приоритету
        critical = [
            i for i in all_manual_items if i.startswith("🔴")
        ]
        important = [
            i for i in all_manual_items if i.startswith("🟡")
        ]
        normal = [
            i for i in all_manual_items if i.startswith("🔵")
        ]
        info = [
            i for i in all_manual_items if i.startswith("⚪")
        ]

        if critical:
            md.append("### 🔴 Critical (blocking)\n")
            for item in critical:
                md.append(f"- {item}")
            md.append("")

        if important:
            md.append("### 🟡 Important (functionality)\n")
            for item in important:
                md.append(f"- {item}")
            md.append("")

        if normal:
            md.append("### 🔵 Normal\n")
            for item in normal:
                md.append(f"- {item}")
            md.append("")

        if info:
            md.append("### ⚪ Info (skipped components)\n")
            for item in info:
                md.append(f"- {item}")
            md.append("")
    else:
        md.append("🎉 No manual intervention required.\n")

    # ── Library Mapping ──
    md.append("## 🔄 Library Mapping\n")
    md.append("| Java / Spring | Go Equivalent |")
    md.append("|--------------|---------------|")
    for java_lib, go_lib in library_mapping:
        md.append(f"| {java_lib} | {go_lib} |")
    md.append("")

    # ── AI Usage ──
    md.append("## 🤖 AI Usage Disclosure\n")
    md.append(
        "This migration uses AI (LLM) for specific stages. "
        "Here's exactly where:\n"
    )
    md.append("| Stage | Method | AI Used | What to Review |")
    md.append("|-------|--------|---------|---------------|")
    md.append(
        "| 1. Java Parsing | JavaParser AST | ❌ No | — |"
    )
    md.append(
        "| 2. Migration Plan | LLM | ✅ Yes | "
        "Review plan for completeness |"
    )
    md.append(
        "| 3. Data Layer (models) | LLM | ✅ Yes | "
        "Verify struct fields and types |"
    )
    md.append(
        "| 4. Business Logic | LLM | ✅ Yes | "
        "**Review all TODO comments** |"
    )
    md.append(
        "| 5. API Layer (handlers) | Template | ❌ No | "
        "Add business logic to handlers |"
    )
    md.append(
        "| 6. Consolidation | Deterministic | ❌ No | — |"
    )
    md.append(
        "| 7. Verification | Deterministic | ❌ No | — |"
    )
    md.append(
        "| 8. Build Check | `go build` | ❌ No | — |"
    )
    md.append(
        "| 9. Reporting | Template | ❌ No | — |"
    )
    md.append("")
    md.append(
        "> **Key insight:** Stages 3 and 4 use AI. "
        "All generated structs and service methods should be "
        "reviewed by an engineer. Stages 5-9 are fully "
        "deterministic and produce compilable code.\n"
    )

    # ── Next Steps ──
    md.append("## 🛠️ Next Steps\n")
    md.append(
        "### Immediate (get it running)\n"
    )
    md.append("1. `cd output/go_project`")
    md.append("2. `go mod tidy && go build ./...`")
    md.append("3. `./main` — verify health check at `http://localhost:8080/health`")
    md.append("")
    md.append("### Short-term (make it work)\n")
    md.append("4. Review and fix `stubs_generated.go` — replace empty structs with real types")
    md.append("5. Implement business logic in `handlers.go` (search for `// TODO`)")
    md.append("6. Implement service methods (search for `// implementation`)")
    md.append("7. Configure database connection (environment variables)")
    md.append("")
    md.append("### Medium-term (make it production-ready)\n")
    md.append("8. Write unit tests for handlers and services")
    md.append("9. Write integration tests — compare Java vs Go responses")
    md.append("10. Add proper logging (zerolog/slog)")
    md.append("11. Add metrics (Prometheus)")
    md.append("12. Deploy with `Dockerfile` (included)")
    md.append("")

    # ── Migration Plan (collapsible) ──
    if migration_plan:
        md.append("## 📋 Migration Plan\n")
        md.append("<details>")
        md.append("<summary>Click to expand</summary>\n")
        md.append(migration_plan)
        md.append("\n</details>\n")

    # ── TODO Details (collapsible) ──
    if todo_details:
        md.append("## 📝 All TODO Comments\n")
        md.append("<details>")
        md.append(
            f"<summary>Show all {len(todo_details)} TODOs</summary>\n"
        )
        for td in todo_details:
            md.append(td)
        md.append("\n</details>\n")

    lint_fixes = state.get("lint_fixes_applied", [])
    if lint_fixes:
        md.append("## 🔧 Linter Auto-fixes\n")
        md.append(f"`golangci-lint` автоматически исправил {len(lint_fixes)} проблем:\n")
        for fix in lint_fixes[:15]:
            md.append(f"- {fix}")
        if len(lint_fixes) > 15:
            md.append(f"\n... и ещё {len(lint_fixes) - 15} исправлений")
        md.append("")

    return "\n".join(md)


# ═══════════════════════════════════════════════════
# Вспомогательные файлы
# ═══════════════════════════════════════════════════

def _write_go_mod(
    output_dir: str,
    java_structure: Dict,
    generated_go_code: Dict[str, str],
) -> None:
    """Генерирует go.mod если его нет."""
    if "go.mod" in generated_go_code:
        return

    package_name = java_structure.get("package", "migrated-service")
    module_name = (
        package_name.replace(".", "-")
        if package_name else "migrated-service"
    )

    all_content = "\n".join(generated_go_code.values())
    deps = ['\tgithub.com/gin-gonic/gin v1.9.1']

    if "gorm.io" in all_content:
        deps.append('\tgorm.io/gorm v1.25.7')
        deps.append('\tgorm.io/driver/postgres v1.5.7')
    if "github.com/jlaffaye/ftp" in all_content:
        deps.append('\tgithub.com/jlaffaye/ftp v0.2.0')
    if "github.com/pkg/sftp" in all_content:
        deps.append('\tgithub.com/pkg/sftp v1.13.6')
    if "go-playground/validator" in all_content:
        deps.append(
            '\tgithub.com/go-playground/validator/v10 v10.19.0'
        )
    if "zerolog" in all_content:
        deps.append('\tgithub.com/rs/zerolog v1.32.0')

    deps_str = "\n".join(deps)

    go_mod_content = (
        f"module {module_name}\n\n"
        "go 1.22\n\n"
        "require (\n"
        f"{deps_str}\n"
        ")\n"
    )

    go_mod_path = os.path.join(output_dir, "go.mod")
    with open(go_mod_path, "w", encoding="utf-8") as f:
        f.write(go_mod_content)


def _write_dockerfile(output_dir: str) -> None:
    """Генерирует Dockerfile."""
    dockerfile_content = (
        "# Build stage\n"
        "FROM golang:1.22-alpine AS builder\n"
        "\n"
        "WORKDIR /app\n"
        "COPY go.mod go.sum* ./\n"
        "RUN go mod download || true\n"
        "\n"
        "COPY . .\n"
        "RUN CGO_ENABLED=0 GOOS=linux "
        "go build -a -installsuffix cgo -o main .\n"
        "\n"
        "# Runtime stage\n"
        "FROM alpine:3.19\n"
        "\n"
        "RUN apk --no-cache add ca-certificates tzdata\n"
        "\n"
        "WORKDIR /app\n"
        "COPY --from=builder /app/main .\n"
        "\n"
        "EXPOSE 8080\n"
        "\n"
        "HEALTHCHECK --interval=30s --timeout=3s \\\n"
        "    CMD wget -qO- http://localhost:8080/health || exit 1\n"
        "\n"
        'ENTRYPOINT ["./main"]\n'
    )

    filepath = os.path.join(output_dir, "Dockerfile")
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(dockerfile_content)


def _write_readme(
    output_dir: str,
    java_structure: Dict,
) -> None:
    """Генерирует README.md."""
    package_name = java_structure.get("package", "migrated-service")
    module_name = (
        package_name.replace(".", "-")
        if package_name else "migrated-service"
    )

    readme_content = (
        f"# {module_name}\n\n"
        "Migrated from Java Spring Boot to Go.\n\n"
        "## Quick Start\n\n"
        "```bash\n"
        "go mod tidy\n"
        "go build -o main .\n"
        "./main\n"
        "```\n\n"
        "Server starts on port 8080. "
        "Health check: http://localhost:8080/health\n\n"
        "## Docker\n\n"
        "```bash\n"
        f"docker build -t {module_name} .\n"
        f"docker run -p 8080:8080 {module_name}\n"
        "```\n\n"
        "## Reports\n\n"
        "- [report.md](report.md) — Human-readable report\n"
        "- [report.json](report.json) — Machine-readable report\n"
        "- `BUILD_REPORT.md` — Detailed build analysis\n\n"
        "## Project Structure\n\n"
        "```\n"
        "├── main.go              # Entry point\n"
        "├── router.go            # Gin routes & middleware\n"
        "├── handlers.go          # HTTP handlers (add logic here)\n"
        "├── models_*.go          # Data models (structs)\n"
        "├── *_service.go         # Business logic\n"
        "├── stubs_generated.go   # Empty stubs (replace these)\n"
        "├── types_common.go      # Type aliases\n"
        "├── Dockerfile           # Docker build\n"
        "├── report.md            # Migration report\n"
        "└── report.json          # Machine-readable report\n"
        "```\n"
    )

    filepath = os.path.join(output_dir, "README.md")
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(readme_content)


# ═══════════════════════════════════════════════════
# Утилиты
# ═══════════════════════════════════════════════════

def _classify_file(filename: str) -> str:
    """Определяет назначение файла по имени."""
    name = filename.lower()
    if "main" in name:
        return "Entry point"
    if "handler" in name or "controller" in name:
        return "HTTP Handlers"
    if "router" in name or "route" in name:
        return "Router"
    if "model" in name or "dto" in name or "type" in name:
        return "Data models"
    if "service" in name:
        return "Business logic"
    if "repository" in name or "repo" in name:
        return "Data access"
    if "middleware" in name:
        return "Middleware"
    if "config" in name:
        return "Configuration"
    if "test" in name:
        return "Tests"
    if "error" in name or "exception" in name:
        return "Error handling"
    if "stub" in name:
        return "Stubs (need implementation)"
    return "Other"