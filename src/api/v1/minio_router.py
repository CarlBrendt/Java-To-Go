import logging
from minio.error import S3Error
from fastapi import APIRouter, Request, File, UploadFile, Form, BackgroundTasks, HTTPException
from fastapi.responses import JSONResponse, Response

from src.api.v1.schemas import TaskResponse, TaskRequest
from src.settings.config import APISettings

from src.api.v1.tasks import upload_and_maybe_migrate, run_migration_for_user

settings = APISettings()
logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/minio-download-ready-zip")
async def download_ready_zip(
    request: Request,
    user_id: str,
    filename: str | None = None,
):
    service = request.app.state.minio
    bucket_name = settings.minio_bucket

    try:
        data, download_name = await service.get_ready_zip_bytes(
            bucket_name, user_id, filename
        )
    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e)) from e
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except S3Error as e:
        logger.error(f"MinIO get ready zip: {e}")
        raise HTTPException(status_code=502, detail=str(e)) from e

    safe_name = download_name.replace('"', "_")
    return Response(
        content=data,
        media_type="application/zip",
        headers={
            "Content-Disposition": f'attachment; filename="{safe_name}"',
        },
    )


@router.post("/minio-upload-zip")
async def upload_zip_handler(
    request: Request,
    user_id: str,
    file: UploadFile = File(...),
    background_tasks: BackgroundTasks = None,
    auto_migrate: bool = False,
):
    """
    Принимает ZIP файл и запускает фоновую задачу для обработки.
    Если auto_migrate=True — после распаковки запускает Java→Go миграцию.
    """
    service = request.app.state.minio
    bucket_name = settings.minio_bucket

    try:
        file_bytes = await file.read()

        if not file_bytes:
            return {"status": "error", "message": "File is empty"}

        logger.info(
            f"Received ZIP: {file.filename}, "
            f"size: {len(file_bytes)} bytes, "
            f"auto_migrate: {auto_migrate}"
        )

        if background_tasks:
            background_tasks.add_task(
                upload_and_maybe_migrate,
                service=service,
                zip_bytes=file_bytes,
                original_filename=file.filename,
                bucket_name=bucket_name,
                user_id=user_id,
                auto_migrate=auto_migrate,
            )
        else:
            await upload_and_maybe_migrate(
                service=service,
                zip_bytes=file_bytes,
                original_filename=file.filename,
                bucket_name=bucket_name,
                user_id=user_id,
                auto_migrate=auto_migrate,
            )

        return {
            "status": "accepted",
            "message": (
                "File received. Migration started in background..."
                if auto_migrate
                else "File received. Processing in background..."
            ),
            "file_name": file.filename,
            "user_id": user_id,
            "auto_migrate": auto_migrate,
        }

    except Exception as e:
        logger.error(f"Failed to queue background task: {e}")
        return {
            "status": "error",
            "message": f"Failed to process file: {str(e)}",
        }


@router.post("/migrate")
async def migrate_java_project(
    request: Request,
    user_id: str,
    background_tasks: BackgroundTasks = None,
):
    """
    Запускает миграцию Java→Go для уже загруженного проекта пользователя.
    Проект должен быть предварительно загружен через /minio-upload-zip.
    """
    service = request.app.state.minio
    bucket_name = settings.minio_bucket

    if background_tasks:
        background_tasks.add_task(
            run_migration_for_user,
            service=service,
            bucket_name=bucket_name,
            user_id=user_id,
        )
        return {
            "status": "accepted",
            "message": f"Migration started for user {user_id}",
            "user_id": user_id,
        }
    else:
        result = await run_migration_for_user(
            service=service,
            bucket_name=bucket_name,
            user_id=user_id,
        )
        return {
            "status": "completed",
            "user_id": user_id,
            "result": result,
        }


@router.get("/migrate/status")
async def migration_status(
    request: Request,
    user_id: str,
):
    """Проверяет статус миграции — есть ли готовый Go-проект."""
    service = request.app.state.minio
    bucket_name = settings.minio_bucket

    try:
        # Проверяем есть ли report.md в output
        output_prefix = f"{user_id}/output/"
        objects = await service.list_objects(bucket_name, output_prefix)

        if not objects:
            return {
                "status": "not_found",
                "message": "No migration output found. Run /migrate first.",
            }

        has_report = any("report.md" in obj for obj in objects)
        has_go_files = any(obj.endswith(".go") for obj in objects)

        return {
            "status": "completed" if has_report else "in_progress",
            "files_count": len(objects),
            "has_report": has_report,
            "has_go_files": has_go_files,
            "download_url": f"/minio-download-ready-zip?user_id={user_id}",
        }

    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.delete("/minio-delete")
async def delete_file(
    request: Request,
    object_name: str,
    background_tasks: BackgroundTasks = None,
):
    service = request.app.state.minio

    try:
        if background_tasks:
            background_tasks.add_task(
                service.delete_file,
                settings.minio_bucket,
                object_name,
            )
            return {
                "status": "accepted",
                "message": f"Deletion of {object_name} queued.",
            }

        await service.delete_file(settings.minio_bucket, object_name)
        return {
            "status": "success",
            "message": f"File {object_name} deleted",
        }

    except Exception as e:
        logger.error(f"Delete failed: {e}")
        return {"status": "error", "message": str(e)}


@router.delete("/minio-delete-user")
async def delete_user_files(
    request: Request,
    user_id: str,
    background_tasks: BackgroundTasks = None,
):
    service = request.app.state.minio

    try:
        if background_tasks:
            background_tasks.add_task(
                service.delete_user_folder,
                settings.minio_bucket,
                user_id,
            )
            return {
                "status": "accepted",
                "message": f"Deletion of user {user_id} data queued.",
            }

        await service.delete_user_folder(
            settings.minio_bucket, user_id
        )
        return {
            "status": "success",
            "message": f"All files for user {user_id} deleted",
        }

    except Exception as e:
        logger.error(f"User delete failed: {e}")
        return {"status": "error", "message": str(e)}

# эндпоинт для возврата zip файла

# не удалять кодовую базу и провести тестирование по причине падения и редачить сам себя
# брал логи с автотестамми и поправлял себя