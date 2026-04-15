import logging
import zipfile
from fastapi.responses import JSONResponse
from fastapi import APIRouter, Request, File, UploadFile, Form

from src.api.v1.schemas import TaskResponse, TaskRequest
from src.settings.config import APISettings

settings = APISettings()
logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/ping-agent")
async def ping_task(request: TaskRequest) -> TaskResponse:

    try:
        response = f"Ping Agents Task for {request.zip}"
        return TaskResponse(task_id=1, task_result=response)
    
    except Exception as e:
        logger.error("Something went wrong")
        return JSONResponse(status_code=200, content={"message": f"There is an error {e}"})