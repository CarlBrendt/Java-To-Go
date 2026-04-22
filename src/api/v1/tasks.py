from __future__ import annotations

import asyncio
import io
import json
import logging
import os
import shutil
import tempfile
from datetime import datetime, timezone
from typing import List, Dict

from minio.error import S3Error

from src.settings.config import APISettings

settings = APISettings()
logger = logging.getLogger(__name__)


async def migration_status_object_key(user_id: str) -> str:
    """Ключ JSON в MinIO с фазой миграции (running / failed)."""
    return f"meta/user_{user_id}/migration_status.json"


async def _put_migration_status_json(
    service,
    bucket_name: str,
    object_key: str,
    payload: dict,
) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    loop = asyncio.get_running_loop()

    def _sync() -> None:
        service.client.put_object(
            bucket_name,
            object_key,
            io.BytesIO(body),
            len(body),
        )

    await loop.run_in_executor(None, _sync)


async def _delete_migration_status_object(
    service,
    bucket_name: str,
    object_key: str,
) -> None:
    loop = asyncio.get_running_loop()

    def _sync() -> None:
        try:
            service.client.remove_object(bucket_name, object_key)
        except S3Error as e:
            if getattr(e, "code", None) not in ("NoSuchKey",):
                raise

    await loop.run_in_executor(None, _sync)


async def upload_and_maybe_migrate(
    service,
    zip_bytes: bytes,
    original_filename: str,
    bucket_name: str,
    user_id: str,
    auto_migrate: bool = False,
    mws_model_name: str | None = None,
):
    """Загружает ZIP, распаковывает, и опционально запускает миграцию."""
    await service.upload_zip_and_extract_in_memory(
        zip_bytes=zip_bytes,
        original_filename=original_filename,
        bucket_name=bucket_name,
        user_id=user_id,
    )

    if auto_migrate:
        await run_migration_for_user(
            service=service,
            bucket_name=bucket_name,
            user_id=user_id,
            mws_model_name=mws_model_name,
        )


async def run_migration_for_user(
    service,
    bucket_name: str,
    user_id: str,
    mws_model_name: str | None = None,
) -> dict:
    """
    Скачивает Java-проект(ы) из MinIO → мигрирует каждый → загружает результат.
    Поддерживает несколько проектов в одном ZIP.
    """
    tmp_dir = None
    marker_key: str | None = None
    marker_active = False
    try:
        tmp_dir = tempfile.mkdtemp(prefix=f"migration_{user_id}_")
        java_dir = os.path.join(tmp_dir, "java_projects")
        output_base = os.path.join(tmp_dir, "go_projects")
        os.makedirs(java_dir, exist_ok=True)
        os.makedirs(output_base, exist_ok=True)

        logger.info(f"Migration started for user {user_id}")

        # 1. Скачиваем файлы из MinIO
        # Файлы лежат в processed/user_{user_id}/
        prefix = f"processed/user_{user_id}/"
        downloaded = await _download_from_minio(
            service, bucket_name, prefix, java_dir
        )
        logger.info(f"Downloaded {downloaded} files for user {user_id}")

        if downloaded == 0:
            return {"status": "error", "message": "No files found in MinIO"}

        # 2. Находим все Java-проекты
        projects = _find_all_java_projects(java_dir)
        logger.info(f"Found {len(projects)} Java project(s): {[p['name'] for p in projects]}")

        if not projects:
            return {"status": "error", "message": "No Java projects found"}

        marker_key = await migration_status_object_key(user_id)
        await _put_migration_status_json(
            service,
            bucket_name,
            marker_key,
            {
                "phase": "running",
                "started_at": datetime.now(timezone.utc).isoformat(),
            },
        )
        marker_active = True

        # 3. Мигрируем каждый проект параллельно
        from src.copilot.run import run_migration

        jar_path = _find_jar()
        results = []

        async def process_project(project):
            project_name = project["name"]
            project_path = project["path"]
            output_dir = os.path.join(output_base, project_name)
            os.makedirs(output_dir, exist_ok=True)

            logger.info(f"Migrating project: {project_name} ({project_path})")

            try:
                result = await run_migration(
                    java_path=project_path,
                    output_dir=output_dir,
                    jar_path=jar_path,
                    mws_model_name=mws_model_name,
                )

                return {
                    "project": project_name,
                    "status": result.get("status", "unknown"),
                    "build_passed": result.get("build_passed", False),
                    "endpoints_migrated": result.get("endpoints_migrated", 0),
                    "files_generated": result.get("files_generated", 0),
                }

            except Exception as e:
                logger.exception(f"Migration failed for {project_name}: {e}")
                return {
                    "project": project_name,
                    "status": "error",
                    "message": str(e),
                }

        # Запускаем все задачи параллельно
        tasks = [process_project(project) for project in projects]
        results = await asyncio.gather(*tasks)

        # 4. Создаём ZIP для каждого проекта и загружаем в ready/
        # 4. Собираем ОДИН ZIP со всеми проектами и загружаем в ready/
        import io as _io
        import zipfile as _zipfile

        zip_buffer = _io.BytesIO()
        total_files = 0

        with _zipfile.ZipFile(zip_buffer, 'w', _zipfile.ZIP_DEFLATED) as zf:
            for project in projects:
                project_name = project["name"]
                local_output = os.path.join(output_base, project_name)

                if not os.path.exists(local_output):
                    continue

                for dirpath, _, filenames in os.walk(local_output):
                    for filename in filenames:
                        local_path = os.path.join(dirpath, filename)
                        # Путь в ZIP: project_name/file.go
                        arcname = os.path.join(
                            project_name,
                            os.path.relpath(local_path, local_output),
                        )
                        zf.write(local_path, arcname)
                        total_files += 1

        zip_bytes = zip_buffer.getvalue()
        zip_name = f"user_{user_id}.zip"
        object_name = f"ready/user_{user_id}/{zip_name}"

        loop = asyncio.get_running_loop()
        await loop.run_in_executor(
            None,
            lambda obj=object_name, data=zip_bytes: service.client.put_object(
                bucket_name,
                obj,
                _io.BytesIO(data),
                len(data),
            )
        )

        if marker_key:
            await _delete_migration_status_object(service, bucket_name, marker_key)
        marker_active = False

        logger.info(
            f"Uploaded {object_name} "
            f"({total_files} files, {len(zip_bytes)} bytes, "
            f"{len(projects)} projects)"
        )

        return {
            "status": "completed",
            "projects_count": len(projects),
            "total_files": total_files,
            "zip_size_bytes": len(zip_bytes),
            "zip_name": zip_name,
            "results": results,
        }

    except Exception as e:
        if marker_active and marker_key:
            try:
                await _put_migration_status_json(
                    service,
                    bucket_name,
                    marker_key,
                    {
                        "phase": "failed",
                        "error": str(e)[:2000],
                        "finished_at": datetime.now(timezone.utc).isoformat(),
                    },
                )
            except Exception as w:
                logger.warning("Could not write migration failed marker: %s", w)
        logger.exception(f"Migration failed for user {user_id}: {e}")
        return {"status": "error", "message": str(e)}

    finally:
        if tmp_dir and os.path.exists(tmp_dir):
            shutil.rmtree(tmp_dir, ignore_errors=True)


def _find_all_java_projects(base_dir: str) -> List[Dict[str, str]]:
    """
    Находит все Java-проекты в директории.
    
    Поддерживает:
    - Один проект в корне (src/ или pom.xml)
    - Несколько проектов в подпапках (p1/, p2/, p3/)
    - Вложенные проекты (folder/project/)
    """
    projects = []

    # Проверяем корень — один проект
    if _is_java_project(base_dir):
        name = os.path.basename(base_dir)
        if name in ("java_projects", "java_project", "raw"):
            name = "main"
        projects.append({"name": name, "path": base_dir})
        return projects

    # Проверяем подпапки — несколько проектов
    for item in sorted(os.listdir(base_dir)):
        item_path = os.path.join(base_dir, item)
        if not os.path.isdir(item_path):
            continue

        if _is_java_project(item_path):
            projects.append({"name": item, "path": item_path})
        else:
            # Ищем на уровень глубже
            for sub_item in sorted(os.listdir(item_path)):
                sub_path = os.path.join(item_path, sub_item)
                if os.path.isdir(sub_path) and _is_java_project(sub_path):
                    projects.append({
                        "name": f"{item}__{sub_item}",
                        "path": sub_path,
                    })

    # Если ничего не нашли — ищем любые .java файлы
    if not projects:
        for dirpath, _, filenames in os.walk(base_dir):
            has_java = any(f.endswith(".java") for f in filenames)
            if has_java:
                # Поднимаемся до папки с src/
                root = _find_project_root_up(dirpath, base_dir)
                name = os.path.relpath(root, base_dir).replace(os.sep, "__")
                if name == ".":
                    name = "main"

                # Не добавляем дубликаты
                if not any(p["path"] == root for p in projects):
                    projects.append({"name": name, "path": root})

    return projects


def _is_java_project(path: str) -> bool:
    """Проверяет что папка — Java-проект."""
    markers = ["pom.xml", "build.gradle", "build.gradle.kts"]

    # Есть build-файл
    for marker in markers:
        if os.path.exists(os.path.join(path, marker)):
            return True

    # Есть src/main/java
    if os.path.exists(os.path.join(path, "src", "main", "java")):
        return True

    # Есть src/ с .java файлами
    src_dir = os.path.join(path, "src")
    if os.path.isdir(src_dir):
        for _, _, files in os.walk(src_dir):
            if any(f.endswith(".java") for f in files):
                return True

    return False


def _find_project_root_up(java_dir: str, stop_at: str) -> str:
    """Поднимается вверх от папки с .java файлами до корня проекта."""
    current = java_dir

    while current != stop_at and current != os.path.dirname(current):
        parent = os.path.dirname(current)

        # Если parent содержит src/ — это корень
        if os.path.basename(current) == "src":
            return parent

        # Если parent содержит pom.xml
        if os.path.exists(os.path.join(parent, "pom.xml")):
            return parent

        current = parent

    return java_dir


def _find_jar() -> str:
    """Находит JavaParser JAR."""
    jar_name = "javaparser-tools-1.0-SNAPSHOT.jar"
    candidates = [
        os.path.join(
            os.path.dirname(os.path.dirname(os.path.dirname(
                os.path.dirname(__file__)
            ))),
            "java-tools", "target", jar_name,
        ),
        os.path.join(os.getcwd(), "java-tools", "target", jar_name),
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return ""


async def _download_from_minio(
    service,
    bucket_name: str,
    prefix: str,
    local_dir: str,
) -> int:
    """Скачивает файлы из MinIO в локальную директорию."""
    import asyncio

    loop = asyncio.get_running_loop()

    def _sync_download():
        count = 0
        try:
            objects = service.client.list_objects(
                bucket_name, prefix=prefix, recursive=True
            )

            for obj in objects:
                obj_name = obj.object_name
                relative_path = obj_name[len(prefix):]
                if not relative_path or obj_name.endswith('/'):
                    continue

                local_path = os.path.join(local_dir, relative_path)
                os.makedirs(os.path.dirname(local_path), exist_ok=True)

                response = service.client.get_object(bucket_name, obj_name)
                try:
                    with open(local_path, "wb") as f:
                        f.write(response.read())
                    count += 1
                finally:
                    response.close()
                    response.release_conn()

        except Exception as e:
            logger.error(f"Download from MinIO failed: {e}")

        return count

    return await loop.run_in_executor(None, _sync_download)


async def _upload_to_minio(
    service,
    bucket_name: str,
    prefix: str,
    local_dir: str,
) -> int:
    """Загружает файлы из локальной директории в MinIO."""
    import asyncio
    import io

    loop = asyncio.get_running_loop()

    def _sync_upload():
        count = 0
        try:
            for dirpath, _, filenames in os.walk(local_dir):
                for filename in filenames:
                    local_path = os.path.join(dirpath, filename)
                    relative_path = os.path.relpath(local_path, local_dir)
                    object_name = f"{prefix}{relative_path}"

                    with open(local_path, "rb") as f:
                        data = f.read()

                    service.client.put_object(
                        bucket_name,
                        object_name,
                        io.BytesIO(data),
                        len(data),
                    )
                    count += 1

        except Exception as e:
            logger.error(f"Upload to MinIO failed: {e}")

        return count

    return await loop.run_in_executor(None, _sync_upload)