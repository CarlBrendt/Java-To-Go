from __future__ import annotations

import asyncio
import logging
import os
import sys

from src.copilot.graph import build_migration_graph
from src.settings.config import APISettings

logger = logging.getLogger(__name__)


async def run_migration(
    java_path: str,
    output_dir: str = "output/go_project",
    jar_path: str = "",
    mws_model_name: str | None = None,
) -> dict:
    """
    Запускает миграционный pipeline.
    Может вызываться как из CLI, так и из API.
    
    Returns:
        dict с результатами миграции
    """
    settings = APISettings()

    # Ищем JAR если не указан
    if not jar_path:
        project_root = os.path.dirname(
            os.path.dirname(os.path.dirname(__file__))
        )
        jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
        candidates = [
            os.path.join(project_root, "java-tools", "target", jar_name),
            os.path.join(os.getcwd(), "java-tools", "target", jar_name),
        ]
        for candidate in candidates:
            if os.path.exists(candidate):
                jar_path = candidate
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

    final_state = {}

    async for event in graph.astream(
        initial_state,
        config={"recursion_limit": 50},
    ):
        for node_name, node_output in event.items():
            current = node_output.get("current_node", node_name)
            status = node_output.get("status", "")
            logger.info(f"  [{current}] → status: {status}")

            if current == "parse":
                controllers = len(
                    node_output.get("java_structure", {}).get("controllers", [])
                )
                dtos = len(
                    node_output.get("java_structure", {}).get("dtos", [])
                )
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
                logger.info(
                    f"    Output saved to: {output_dir}"
                )

            final_state = node_output

    logger.info("=" * 60)
    logger.info("Migration pipeline complete!")

    # Собираем результат
    result = {
        "status": final_state.get("status", "unknown"),
        "build_passed": final_state.get("build_passed", False),
        "verification_passed": final_state.get("verification_passed", False),
        "build_errors": final_state.get("build_errors", []),
        "build_fixes_applied": final_state.get("build_fixes_applied", []),
        "endpoints_migrated": len([
            e for e in final_state.get("api_contract", [])
            if not e.get("is_exception_handler")
        ]),
        "files_generated": len(final_state.get("generated_go_code", {})),
        "output_dir": output_dir,
    }

    return result


def main():
    """CLI entry point."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    java_path = sys.argv[1] if len(sys.argv) > 1 else "workflow-ftp"
    output = sys.argv[2] if len(sys.argv) > 2 else "output/go_project"

    jar_path = ""
    project_root = os.path.dirname(
        os.path.dirname(os.path.dirname(__file__))
    )
    jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
    candidates = [
        os.path.join(project_root, "java-tools", "target", jar_name),
        os.path.join(os.getcwd(), "java-tools", "target", jar_name),
    ]
    for candidate in candidates:
        if os.path.exists(candidate):
            jar_path = candidate
            break

    result = asyncio.run(
        run_migration(java_path, output, jar_path)
    )

    report_path = os.path.join(output, "report.md")
    json_path = os.path.join(output, "report.json")
    logger.info(f"Report: {report_path}")
    logger.info(f"JSON:   {json_path}")


if __name__ == "__main__":
    main()