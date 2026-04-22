# -*- coding: utf-8 -*-
"""
migration_cli.py

CLI‑утилита, которая ищет все подпапки‑проекты Java внутри каталога
`lowcode` (на том же уровне, что и `src`), запускает миграцию в
параллельных корутинах и сохраняет результаты в отдельные папки.

Запуск:
    python -m src.copilot.migration_cli           # использует ./lowcode
    python -m src.copilot.migration_cli /path/to/lowcode /path/to/output
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import sys
from pathlib import Path
from typing import List, Dict, Any

# --------------------------------------------------------------------------- #
#   Local imports (оставляем относительные пути, как в оригинальном проекте)
# --------------------------------------------------------------------------- #
from src.copilot.graph import build_migration_graph
from src.settings.config import APISettings

# --------------------------------------------------------------------------- #
#   Logger
# --------------------------------------------------------------------------- #
logger = logging.getLogger(__name__)

# --------------------------------------------------------------------------- #
#   Helper: discover Java projects inside a directory
# --------------------------------------------------------------------------- #
def discover_projects(root: Path) -> List[Path]:
    """
    Возвращает список подпапок, которые выглядят как Java‑проекты.

    Признаком проекта считается наличие каталога ``src/main/java`` либо
    файлов сборки Maven/Gradle (``pom.xml`` / ``build.gradle``).

    Parameters
    ----------
    root: Path
        Папка, в которой нужно искать проекты.

    Returns
    -------
    List[Path]
        Список найденных проектов.
    """
    candidates: List[Path] = []
    for entry in root.iterdir():
        if not entry.is_dir():
            continue

        if (entry / "src" / "main" / "java").exists():
            candidates.append(entry)
        elif (entry / "pom.xml").exists() or (entry / "build.gradle").exists():
            candidates.append(entry)

    return candidates


# --------------------------------------------------------------------------- #
#   Helper: create per‑project output directory
# --------------------------------------------------------------------------- #
def make_output_dir(global_out: Path, project_path: Path) -> Path:
    """
    Создаёт (если нужно) подкаталог в ``global_out`` с тем же именем,
    что и у проекта, и возвращает путь к нему.
    """
    out = global_out / project_path.name
    out.mkdir(parents=True, exist_ok=True)
    return out


# --------------------------------------------------------------------------- #
#   Core async function – почти копия из вашего оригинального файла
# --------------------------------------------------------------------------- #
async def run_migration(
    java_path: str,
    output_dir: str = "output/go_project",
    jar_path: str = "",
    mws_model_name: str | None = None,
) -> dict:
    """Запускает миграционный pipeline и возвращает сводку."""
    settings = APISettings()

    # Если JAR не передан – ищем в типовых местах
    if not jar_path:
        project_root = Path(__file__).resolve().parents[2]  # ../../..
        jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
        candidates = [
            project_root / "java-tools" / "target" / jar_name,
            Path.cwd() / "java-tools" / "target" / jar_name,
        ]
        for candidate in candidates:
            if candidate.exists():
                jar_path = str(candidate)
                break

    logger.info(f"JavaParser JAR: {jar_path}")
    logger.info(f"JAR exists: {os.path.exists(jar_path) if jar_path else False}")
    logger.info(f"Starting migration: {java_path} → {output_dir}")
    logger.info("=" * 60)

    graph = build_migration_graph(settings)

    initial_state = {
        "java_project_path": java_path,
        "output_dir": output_dir,
        "jar_path": jar_path,
        "mws_model_name": mws_model_name,
        "status": "starting",
        "current_node": "",
        "generated_go_code": {},
    }

    final_state: dict = {}

    async for event in graph.astream(initial_state, config={"recursion_limit": 50}):
        for node_name, node_output in event.items():
            current = node_output.get("current_node", node_name)
            status = node_output.get("status", "")
            logger.info(f"  [{current}] → status: {status}")

            if current == "parse":
                controllers = len(
                    node_output.get("java_structure", {}).get("controllers", [])
                )
                dtos = len(node_output.get("java_structure", {}).get("dtos", []))
                services = len(
                    node_output.get("java_structure", {}).get("services", [])
                )
                logger.info(
                    f"    Found: {controllers} controllers, "
                    f"{dtos} DTOs, {services} services"
                )

            elif current == "verify":
                passed = node_output.get("verification_passed", False)
                errors = node_output.get("verification_errors", [])
                fixes = node_output.get("manual_fixes", [])
                logger.info(
                    f"    Verification: "
                    f"{'PASSED ✅' if passed else 'FAILED ❌'} "
                    f"({len(errors)} errors, {len(fixes)} manual fixes)"
                )
                for err in errors[:3]:
                    logger.info(f"      → {err}")

            elif current == "build_check":
                passed = node_output.get("build_passed", False)
                errors = node_output.get("build_errors", [])
                fixes = node_output.get("build_fixes_applied", [])
                logger.info(
                    f"    Build: "
                    f"{'PASSED ✅' if passed else 'FAILED ❌'} "
                    f"({len(errors)} errors, {len(fixes)} auto-fixes)"
                )
                for fix in fixes[:5]:
                    logger.info(f"      fix: {fix}")
                if not passed and errors:
                    for err in errors[:5]:
                        logger.info(f"      err: {err}")

            elif current == "reporting_packaging":
                logger.info(f"    Output saved to: {output_dir}")

            final_state = node_output

    logger.info("=" * 60)
    logger.info("Migration pipeline complete!")

    # Сводка
    result = {
        "status": final_state.get("status", "unknown"),
        "build_passed": final_state.get("build_passed", False),
        "verification_passed": final_state.get("verification_passed", False),
        "build_errors": final_state.get("build_errors", []),
        "build_fixes_applied": final_state.get("build_fixes_applied", []),
        "endpoints_migrated": len(
            [
                e
                for e in final_state.get("api_contract", [])
                if not e.get("is_exception_handler")
            ]
        ),
        "files_generated": len(final_state.get("generated_go_code", {})),
        "output_dir": output_dir,
    }

    return result


# --------------------------------------------------------------------------- #
#   Async worker – runs a single migration under a semaphore
# --------------------------------------------------------------------------- #
async def migrate_one(
    project_path: Path,
    output_dir: Path,
    jar_path: str,
    semaphore: asyncio.Semaphore,
) -> Dict[str, Any]:
    """Запускает run_migration, защищённый семафором."""
    async with semaphore:
        try:
            result = await run_migration(
                java_path=str(project_path),
                output_dir=str(output_dir),
                jar_path=jar_path,
            )
            return {"project": project_path.name, "result": result}
        except Exception as exc:  # noqa: BLE001
            logger.exception("❌ Migration failed for %s", project_path)
            return {"project": project_path.name, "error": str(exc)}


# --------------------------------------------------------------------------- #
#   Main entry point
# --------------------------------------------------------------------------- #
def main() -> None:
    """CLI‑точка входа – ищет проекты в каталоге `lowcode` и мигрирует их."""
    # Настройка логгера
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    # 2 Параметры CLI
    #    argv[1] – путь к каталогу с проектами (по‑умолчанию ./lowcode)
    #    argv[2] – глобальная папка вывода (по‑умолчанию ./output/go_projects)
    cwd = Path.cwd()
    default_lowcode = cwd / "lowcode"          # <-- именно эта папка
    root_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else default_lowcode
    global_output = Path(sys.argv[2]) if len(sys.argv) > 2 else cwd / "output/go_projects"
    global_output.mkdir(parents=True, exist_ok=True)

    logger.info(f"🔎 Searching Java projects in: {root_dir}")

    # 3 Находим JAR один раз
    project_root = Path(__file__).resolve().parents[2]  # ../../..
    jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
    jar_candidates = [
        project_root / "java-tools" / "target" / jar_name,
        cwd / "java-tools" / "target" / jar_name,
    ]
    jar_path = next((str(p) for p in jar_candidates if p.exists()), "")
    logger.info(f"🔧 JavaParser JAR selected: {jar_path}")

    # 4 Находим все Java‑проекты внутри `root_dir`
    projects = discover_projects(root_dir)
    if not projects:
        logger.warning("⚠️  No Java projects found in %s", root_dir)
        return

    logger.info("🚀 Found %d project(s) to migrate:", len(projects))
    for p in projects:
        logger.info("   - %s", p)

    # 5 Параллелизм (по умолчанию 4, можно переопределить через env)
    max_concurrent = int(os.getenv("MAX_CONCURRENT", "4"))
    semaphore = asyncio.Semaphore(max_concurrent)

    async def run_all() -> List[Dict[str, Any]]:
        tasks = [
            migrate_one(
                project_path=p,
                output_dir=make_output_dir(global_output, p),
                jar_path=jar_path,
                semaphore=semaphore,
            )
            for p in projects
        ]
        return await asyncio.gather(*tasks)

    # 6 Запуск
    results = asyncio.run(run_all())

    # 7 Общий summary.json
    summary_path = global_output / "summary.json"
    summary_path.write_text(json.dumps(results, ensure_ascii=False, indent=2))
    logger.info("📊 Summary written to %s", summary_path)

    # 8 Пер‑проектные отчёты (markdown + json)
    for item in results:
        proj_name = item["project"]
        proj_out = global_output / proj_name
        md_path = proj_out / "report.md"
        json_path = proj_out / "report.json"

        if "result" in item:
            res = item["result"]
            md_path.write_text(
                f"# Migration report for `{proj_name}`\n\n"
                f"- **Status**: {res.get('status')}\n"
                f"- **Build passed**: {res.get('build_passed')}\n"
                f"- **Verification passed**: {res.get('verification_passed')}\n"
                f"- **Endpoints migrated**: {res.get('endpoints_migrated')}\n"
                f"- **Files generated**: {res.get('files_generated')}\n"
                f"- **Output directory**: `{res.get('output_dir')}`\n"
            )
            json_path.write_text(json.dumps(res, ensure_ascii=False, indent=2))
            logger.info("✅ Report for %s → %s", proj_name, md_path)
        else:
            err_msg = item.get("error", "unknown error")
            md_path.write_text(
                f"# ❌ Migration FAILED for `{proj_name}`\n\n"
                f"```\n{err_msg}\n```"
            )
            json_path.write_text(json.dumps({"error": err_msg}, ensure_ascii=False, indent=2))
            logger.error("❌ Migration failed for %s → %s", proj_name, md_path)


if __name__ == "__main__":
    main()