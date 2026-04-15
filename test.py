"""
Диагностика полей классов.
python3 test_fields.py workflow-ftp
"""
import json
import os
import sys
from pathlib import Path

PROJECT_ROOT = str(Path(__file__).parent)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from src.copilot.nodes.analysis_node import JavaParser


def main():
    java_path = sys.argv[1] if len(sys.argv) > 1 else "workflow-ftp"
    jar_path = "/Users/pmedvedeva/Desktop/Projects/task-repo/java-tools/target/javaparser-tools-1.0-SNAPSHOT.jar"

    parser = JavaParser(jar_path)

    # Ищем только контроллеры и сервисы
    target_classes = [
        "FtpCallConfigValidationApiImpl",
        "FtpExecutionApiImpl",
        "GlobalControllerExceptionHandler",
        "SwaggerRediraction",
        "ScriptExecutorServiceImpl",
        "FtpServiceImpl",
        "SftpServiceImpl",
        "RemoteBlobStorage",
        "ValidationServiceImpl",
        "FtpCallActivityImpl",
        "LocalizationConfiguration",
        "SecurityConfiguration",
    ]

    for dirpath, dirnames, filenames in os.walk(java_path):
        dirnames[:] = [d for d in dirnames if d not in {"target", ".git", "build", "test"}]
        for filename in filenames:
            if not filename.endswith(".java"):
                continue
            filepath = os.path.join(dirpath, filename)
            result = parser.parse_file_content(filepath)
            if not result:
                continue

            for cls in result.get("classes", []):
                cls_name = cls.get("class_name", "")
                if cls_name not in target_classes:
                    continue

                annotations = cls.get("annotations", [])
                fields = cls.get("fields", [])
                implements = cls.get("implements", [])

                print(f"\n{'='*60}")
                print(f"CLASS: {cls_name}")
                print(f"  Annotations: {annotations}")
                print(f"  Implements: {implements}")
                print(f"  Fields ({len(fields)}):")

                for f in fields:
                    print(f"    {json.dumps(f, ensure_ascii=False)}")

                if not fields:
                    print(f"    (no fields)")

                # Проверяем injected_dependencies
                injected = cls.get("injected_dependencies", [])
                if injected:
                    print(f"  injected_dependencies: {injected}")


if __name__ == "__main__":
    main()