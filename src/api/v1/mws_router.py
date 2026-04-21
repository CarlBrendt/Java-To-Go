"""MWS GPT: список моделей для UI и общие утилиты."""
import logging

import httpx
from fastapi import APIRouter, HTTPException

from src.settings.config import APISettings

logger = logging.getLogger(__name__)
router = APIRouter()
settings = APISettings()


async def _is_suitable_migration_llm(model_id: str) -> bool:
    """Только id из _MIGRATION_LLM_ALLOWLIST (регистр не важен)."""
    s = model_id.strip().lower()
    return bool(s) and s in settings.migration_llm_allowlist


async def _filter_models_payload(payload: dict) -> dict:
    data = payload.get("data")
    if not isinstance(data, list):
        return payload
    filtered = [m for m in data if isinstance(m, dict) and await _is_suitable_migration_llm(str(m.get("id", "")))]
    out = {**payload, "data": filtered}
    return out


async def _check_mws_config() -> None:
    if not settings.resolve_mws_base_url():
        raise HTTPException(
            status_code=503,
            detail="Задайте MWS_GPT_BASE_URL (например https://api.gpt.mws.ru/v1)",
        )
    if not (settings.model_api_key or "").strip():
        raise HTTPException(
            status_code=503,
            detail="Задайте MODEL_API_KEY (sk-...)",
        )


@router.get("/models")
async def list_mws_models():
    """
    Прокси к GET {MWS_GPT_BASE_URL}/models — список id моделей для выбора на фронте.
    Тот же Bearer, что и для чат-запросов.
    """
    await _check_mws_config()
    base = settings.resolve_mws_base_url()
    url = f"{base}/models"
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            r = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {settings.model_api_key}",
                    "Content-Type": "application/json",
                },
            )
            if r.status_code >= 400:
                raise HTTPException(
                    status_code=r.status_code,
                    detail=r.text[:2000],
                )
            body = r.json()
            return await _filter_models_payload(body)
    except httpx.RequestError as e:
        logger.exception("MWS /models request failed")
        raise HTTPException(status_code=502, detail=str(e)) from e
