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

        # Ищем во ВСЁМ сгенерированном коде
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

    # ── 4. Объединяем manual fixes ──
    all_manual_items: List[str] = []
    all_manual_items.extend(manual_fixes)
    all_manual_items.extend(todo_details)

    for ep in endpoints_missing:
        all_manual_items.append(
            f"Реализовать эндпоинт `{ep['method']} {ep['path']}` "
            f"(Java handler: `{ep['handler']}` в `{ep['class']}`)"
        )

    # Дедупликация
    seen = set()
    unique_items: List[str] = []
    for item in all_manual_items:
        normalized = item.strip().lstrip("- ")
        if normalized not in seen:
            seen.add(normalized)
            unique_items.append(item)
    all_manual_items = unique_items

    # ── 5. Маппинг библиотек ──
    library_mapping = [
        ("Spring Boot (@Controller, @Service)", "Go Structs + Methods"),
        ("Spring MVC (@GetMapping, @PostMapping)", "Gin Router"),
        ("Jackson (JSON)", "encoding/json + struct tags"),
        ("Hibernate / JPA", "GORM / sqlx"),
        ("Spring Security", "Gin Middleware"),
        ("Lombok (@Data, @Builder)", "Go Structs (explicit)"),
        ("Bean Validation (@NotNull)", "go-playground/validator"),
        ("SLF4J / Logback", "zerolog / slog"),
        ("Spring @Transactional", "Manual DB transactions"),
        ("Apache Commons Net (FTP)", "github.com/jlaffaye/ftp"),
        ("SSHJ / JSch (SFTP)", "github.com/pkg/sftp"),
    ]

    # ── 6. Статистика ──
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

    # ── 7. Подсчёт файлов по категориям ──
    file_categories = {}
    for filename in generated_go_code:
        cat = _classify_file(filename)
        file_categories[cat] = file_categories.get(cat, 0) + 1

    # ── 8. JSON-отчёт ──
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
        "verification": {
            "status": (
                "passed" if not verification_errors else "failed"
            ),
            "errors": verification_errors,
        },
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

    # ── 9. Markdown-отчёт ──
    md: List[str] = []

    md.append("# 🚀 Java → Go Migration Report\n")
    md.append(
        f"**Generated:** "
        f"{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  "
    )
    md.append(
        f"**Package:** "
        f"`{java_structure.get('package', 'unknown')}`\n"
    )

    # Executive Summary
    md.append("## 📊 Executive Summary\n")
    md.append("| Metric | Value |")
    md.append("|--------|-------|")
    md.append(
        f"| Status | **{report_json['status'].upper()}** |"
    )
    md.append(f"| Java Controllers | {report_json['java_source']['controllers_count']} |")
    md.append(f"| Java Services | {report_json['java_source']['services_count']} |")
    md.append(f"| Java DTOs | {report_json['java_source']['dto_count']} |")
    md.append(f"| Total Endpoints | {total_endpoints} |")
    md.append(f"| Endpoints Migrated | {migrated_count} |")
    md.append(f"| Endpoints Missing | {missing_count} |")
    md.append(f"| Coverage | **{coverage_pct}%** |")
    md.append(f"| Go Files Generated | {len(generated_go_code)} |")
    md.append(f"| Verification | {verification_status} |")
    build_passed = state.get("build_passed", False)
    md.append(
        f"| Build | {'✅ PASSED' if build_passed else '❌ FAILED'} |"
    )
    md.append(f"| Manual Fixes | {len(all_manual_items)} |")
    md.append("")

    # Verification
    md.append("## 🔍 Verification Results\n")
    if not verification_errors:
        md.append("✅ All checks passed.\n")
    else:
        md.append("❌ Issues found:\n")
        for err in verification_errors:
            md.append(f"- {err}")
        md.append("")

    # Build Check
    build_passed = state.get("build_passed", False)
    build_errors = state.get("build_errors", [])
    build_fixes = state.get("build_fixes_applied", [])
    build_report = state.get("build_report", "")

    md.append("## 🔧 Build Check\n")
    md.append(
        f"**Status:** {'✅ PASSED' if build_passed else '❌ FAILED'}"
    )
    md.append(f"**Auto-fixes applied:** {len(build_fixes)}")
    md.append(f"**Remaining errors:** {len(build_errors)}\n")

    if build_fixes:
        md.append("### Auto-fixes Applied\n")
        for fix in build_fixes[:10]:
            md.append(f"- {fix}")
        if len(build_fixes) > 10:
            md.append(f"- ... and {len(build_fixes) - 10} more")
        md.append("")

    if build_errors:
        md.append("### Errors for Engineer\n")
        md.append(
            "> See `BUILD_REPORT.md` for detailed fix instructions.\n"
        )
        md.append("| # | Error |")
        md.append("|---|-------|")
        for i, err in enumerate(build_errors[:20], 1):
            md.append(f"| {i} | `{err}` |")
        if len(build_errors) > 20:
            md.append(
                f"| ... | {len(build_errors) - 20} more errors |"
            )
        md.append("")

    md.append("")

    # Migrated endpoints
    md.append("## ✅ Migrated Endpoints\n")
    if endpoints_migrated:
        md.append("| Method | Path | Handler | Class |")
        md.append("|--------|------|---------|-------|")
        for ep in endpoints_migrated:
            md.append(
                f"| `{ep['method']}` | `{ep['path']}` "
                f"| `{ep['handler']}` | `{ep['class']}` |"
            )
    else:
        md.append("⚠️ No endpoints verified.\n")
    md.append("")

    # Missing endpoints
    md.append("## ❌ Missing Endpoints\n")
    if endpoints_missing:
        md.append("| Method | Path | Handler | Class |")
        md.append("|--------|------|---------|-------|")
        for ep in endpoints_missing:
            md.append(
                f"| `{ep['method']}` | `{ep['path']}` "
                f"| `{ep['handler']}` | `{ep['class']}` |"
            )
        md.append("")
        md.append("> These need manual implementation.\n")
    else:
        md.append("🎉 All endpoints covered!\n")
    md.append("")

    # Generated files
    md.append("## 📦 Generated Files\n")
    md.append("| File | Size | Purpose |")
    md.append("|------|------|---------|")
    for filename, content in generated_go_code.items():
        purpose = _classify_file(filename)
        md.append(
            f"| `{filename}` | {len(content)} chars | {purpose} |"
        )
    md.append("")

    # Manual intervention
    md.append("## ⚠️ Manual Intervention Required\n")
    if all_manual_items:
        md.append(
            f"**{len(all_manual_items)} items** need attention:\n"
        )
        for item in all_manual_items:
            if item.startswith("- "):
                md.append(item)
            else:
                md.append(f"- {item}")
    else:
        md.append("🎉 No manual intervention required.\n")
    md.append("")

    # TODO items
    if todo_details:
        md.append("### 📝 TODO in Generated Code\n")
        for td in todo_details:
            md.append(td)
        md.append("")

    # Library mapping
    md.append("## 🔄 Library Mapping\n")
    md.append("| Java / Spring | Go Equivalent |")
    md.append("|--------------|---------------|")
    for java_lib, go_lib in library_mapping:
        md.append(f"| {java_lib} | {go_lib} |")
    md.append("")

    # AI usage
    md.append("## 🤖 AI Usage Disclosure\n")
    md.append("| Stage | AI | Human |")
    md.append("|-------|----|-------|")
    md.append("| 1. Parsing | ❌ | AST (deterministic) |")
    md.append("| 2. Planning | ✅ | Review plan |")
    md.append("| 3. Data Layer | ✅ | Verify types |")
    md.append("| 4. Business Logic | ✅ | Review TODOs |")
    md.append("| 5. API Layer | ✅ | Fix endpoints |")
    md.append("| 6. Verification | ❌ | Automated |")
    md.append("| 7. Reporting | ❌ | Automated |")
    md.append("")

    # Next steps
    md.append("## 🛠️ Next Steps\n")
    md.append("1. Review files with `TODO` comments.")
    md.append("2. Implement missing endpoints.")
    md.append("3. Run `go mod tidy` and `go build ./...`.")
    md.append("4. Configure database connection.")
    md.append("5. Write integration tests.")
    md.append("6. Compare Java vs Go responses.")
    md.append("7. Deploy with Dockerfile.")
    md.append("")

    # Migration plan (collapsible)
    if migration_plan:
        md.append("## 📋 Migration Plan\n")
        md.append("<details>")
        md.append("<summary>Click to expand</summary>\n")
        md.append(migration_plan)
        md.append("\n</details>\n")

    md_text = "\n".join(md)

    md_report_path = os.path.join(output_dir, "report.md")
    with open(md_report_path, "w", encoding="utf-8") as f:
        f.write(md_text)

    logger.info(
        f"Report generated: {md_report_path} "
        f"({migrated_count}/{total_endpoints} endpoints, "
        f"{len(all_manual_items)} manual fixes)"
    )

    # ── 10. go.mod ──
    package_name = java_structure.get("package", "migrated-service")
    module_name = (
        package_name.replace(".", "-")
        if package_name else "migrated-service"
    )

    if "go.mod" not in generated_go_code:
        # Собираем зависимости из сгенерированного кода
        all_content = "\n".join(generated_go_code.values())
        deps = []
        deps.append('\tgithub.com/gin-gonic/gin v1.9.1')

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

    # ── 11. Dockerfile ──
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
    dockerfile_path = os.path.join(output_dir, "Dockerfile")
    with open(dockerfile_path, "w", encoding="utf-8") as f:
        f.write(dockerfile_content)

    # ── 12. README ──
    readme_content = (
        f"# {module_name}\n\n"
        "Migrated from Java Spring Boot to Go.\n\n"
        "## Quick Start\n\n"
        "```bash\n"
        "go mod tidy\n"
        "go build -o main .\n"
        "./main\n"
        "```\n\n"
        "## Docker\n\n"
        "```bash\n"
        f"docker build -t {module_name} .\n"
        f"docker run -p 8080:8080 {module_name}\n"
        "```\n\n"
        "## Reports\n\n"
        "- [report.md](report.md) — Human-readable report\n"
        "- [report.json](report.json) — Machine-readable report\n"
    )
    readme_path = os.path.join(output_dir, "README.md")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(readme_content)

    return {
        "output_dir": output_dir,
        "status": "reporting_packaging_complete",
        "current_node": "reporting_packaging",
        "report_generated": True,
    }


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
    return "Other"