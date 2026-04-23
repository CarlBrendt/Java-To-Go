from fastapi import APIRouter, HTTPException
import httpx

from src.api.v1.schemas import (
    ValidationRunCreateResponse,
    ValidationRunRequest,
    ValidationRunResponse,
)
from src.settings.config import APISettings
from src.services.validation_orchestrator_client import ValidationOrchestratorClient


router = APIRouter()
settings = APISettings()


def _client() -> ValidationOrchestratorClient:
    return ValidationOrchestratorClient(settings.validation_orchestrator_base_url)


@router.post("/runs", response_model=ValidationRunCreateResponse)
async def start_validation_run(payload: ValidationRunRequest | None = None):
    try:
        return await _client().start_run(payload or ValidationRunRequest())
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=exc.response.status_code,
            detail=exc.response.text,
        ) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Validation orchestrator is unavailable: {exc}",
        ) from exc


@router.get("/runs/{validation_run_id}", response_model=ValidationRunResponse)
async def get_validation_run(validation_run_id: str):
    try:
        return await _client().get_run(validation_run_id)
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=exc.response.status_code,
            detail=exc.response.text,
        ) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Validation orchestrator is unavailable: {exc}",
        ) from exc
