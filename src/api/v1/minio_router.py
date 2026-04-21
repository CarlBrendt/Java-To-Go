import json
import logging
from minio.error import S3Error
from fastapi import APIRouter, Request, File, UploadFile, Form, BackgroundTasks, HTTPException, Query
from fastapi.responses import JSONResponse, Response

from src.api.v1.schemas import TaskResponse, TaskRequest
from src.settings.config import APISettings

from src.api.v1.tasks import (
    migration_status_object_key,
    upload_and_maybe_migrate,
    run_migration_for_user,
)

settings = APISettings()
logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/minio-download-ready-zip")
async def download_ready_zip(
    request: Request,
    user_id: str,
    filename: str | None = None,
) -> JSONResponse:
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

    return JSONResponse(
        content=data,
        status_code=200,
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
    mws_model: str | None = Form(
        default=None,
        description="ID модели MWS для миграции (если auto_migrate=true); см. GET /api/v1/mws/models",
    ),
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
                mws_model_name=mws_model,
            )
        else:
            await upload_and_maybe_migrate(
                service=service,
                zip_bytes=file_bytes,
                original_filename=file.filename,
                bucket_name=bucket_name,
                user_id=user_id,
                auto_migrate=auto_migrate,
                mws_model_name=mws_model,
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
    mws_model: str | None = Query(
        default=None,
        description="ID модели MWS; по умолчанию MODEL_NAME из .env",
    ),
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
            mws_model_name=mws_model,
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
            mws_model_name=mws_model,
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
    """
    Статус миграции по данным MinIO: загруженный архив, распаковка, фаза миграции, готовый zip.
    """
    service = request.app.state.minio
    bucket_name = settings.minio_bucket

    try:
        ready_prefix = f"ready/user_{user_id}/"
        processed_prefix = f"processed/user_{user_id}/"
        raw_prefix = f"raw/user_{user_id}_"
        meta_key = await migration_status_object_key(user_id)

        ready_objs = await service.list_objects(bucket_name, ready_prefix)
        processed_objs = await service.list_objects(bucket_name, processed_prefix)
        raw_objs = await service.list_objects(bucket_name, raw_prefix)

        ready_zips = [o for o in ready_objs if o.lower().endswith(".zip")]
        uploaded_archives = [o for o in raw_objs if o.lower().endswith(".zip")]

        marker_raw = await service.get_object_bytes_if_exists(bucket_name, meta_key)
        marker: dict | None = None
        if marker_raw:
            try:
                marker = json.loads(marker_raw.decode("utf-8"))
            except json.JSONDecodeError:
                marker = {"phase": "unknown", "raw": True}

        phase = (marker or {}).get("phase")

        counts = {
            "uploaded_archives": len(uploaded_archives),
            "extracted_objects": len(processed_objs),
            "ready_output_zips": len(ready_zips),
            "ready_bucket_objects": len(ready_objs),
        }

        has_ready_zip = len(ready_zips) >= 1
        has_report = any("report.md" in obj for obj in ready_objs)
        has_go_files = any(obj.endswith(".go") for obj in ready_objs)

        download_url = f"/api/v1/minio/minio-download-ready-zip?user_id={user_id}"

        # Приоритет: идёт миграция → последняя попытка упала → есть готовый zip → ждёт миграции → пусто
        if phase == "running":
            return {
                "status": "migration_running",
                "message": "Миграция выполняется (LLM / сборка). Повторите запрос позже.",
                "counts": counts,
                "migration": {
                    "phase": "running",
                    "started_at": (marker or {}).get("started_at"),
                },
                "has_ready_zip": has_ready_zip,
                "has_report": has_report,
                "has_go_files": has_go_files,
                "download_url": download_url if has_ready_zip else None,
            }

        if phase == "failed":
            return {
                "status": "migration_failed",
                "message": (marker or {}).get("error", "Миграция завершилась с ошибкой."),
                "counts": counts,
                "migration": {
                    "phase": "failed",
                    "error": (marker or {}).get("error"),
                    "finished_at": (marker or {}).get("finished_at"),
                },
                "has_ready_zip": has_ready_zip,
                "has_report": has_report,
                "has_go_files": has_go_files,
                "download_url": download_url if has_ready_zip else None,
            }

        if has_ready_zip or has_report or has_go_files:
            return {
                "status": "completed",
                "message": "Есть результат миграции в хранилище.",
                "counts": counts,
                "migration": {"phase": "completed"},
                "has_ready_zip": has_ready_zip,
                "has_report": has_report,
                "has_go_files": has_go_files,
                "download_url": download_url,
            }

        if uploaded_archives or processed_objs:
            return {
                "status": "awaiting_migration",
                "message": "Архив загружен и распакован; миграция ещё не дала готовый zip. Запустите POST /migrate или загрузите с auto_migrate=true.",
                "counts": counts,
                "migration": {"phase": "idle"},
                "has_ready_zip": False,
                "has_report": False,
                "has_go_files": False,
                "download_url": None,
            }

        return {
            "status": "not_found",
            "message": "Нет данных пользователя в MinIO (ни загруженного архива, ни результата).",
            "counts": counts,
            "migration": {"phase": "none"},
            "has_ready_zip": False,
            "has_report": False,
            "has_go_files": False,
            "download_url": None,
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