"""MWS GPT: список моделей для UI и общие утилиты."""

from __future__ import annotations

import logging

import httpx
from fastapi import APIRouter, HTTPException

from src.settings.config import APISettings

logger = logging.getLogger(__name__)
router = APIRouter()
_settings = APISettings()


def _is_suitable_migration_llm(model_id: str) -> bool:
    """
    Оставляем модели для текстовой генерации кода/плана (chat / instruct / coder),
    в т.ч. с -vl в id. Исключаем embedding, whisper, bge и т.п.
    """
    s = model_id.strip().lower()
    if not s:
        return False
    if any(
        b in s
        for b in (
            "embedding",
            "whisper",
            "bge-m3",
            "bge-multilingual",
            "/bge",
            "bge-",
        )
    ):
        return False
    if "-vl" in s:
        return True
    if any(
        g in s
        for g in (
            "instruct",
            "coder",
            "chat",
        )
    ):
        return True
    if "-it" in s or s.endswith("-it"):
        return True
    if any(
        g in s
        for g in (
            "gpt-oss",
            "mws-gpt",
            "glm-4",
            "deepseek",
            "kimi",
            "qwq",
        )
    ):
        return True
    if s == "qwen3-32b":
        return True
    return False


def _filter_models_payload(payload: dict) -> dict:
    data = payload.get("data")
    if not isinstance(data, list):
        return payload
    filtered = [m for m in data if isinstance(m, dict) and _is_suitable_migration_llm(str(m.get("id", "")))]
    out = {**payload, "data": filtered}
    return out


def _check_mws_config() -> None:
    if not _settings.resolve_mws_base_url():
        raise HTTPException(
            status_code=503,
            detail="Задайте MWS_GPT_BASE_URL (например https://api.gpt.mws.ru/v1)",
        )
    if not (_settings.model_api_key or "").strip():
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
    _check_mws_config()
    base = _settings.resolve_mws_base_url()
    url = f"{base}/models"
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            r = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {_settings.model_api_key}",
                    "Content-Type": "application/json",
                },
            )
            if r.status_code >= 400:
                raise HTTPException(
                    status_code=r.status_code,
                    detail=r.text[:2000],
                )
            body = r.json()
            return _filter_models_payload(body)
    except httpx.RequestError as e:
        logger.exception("MWS /models request failed")
        raise HTTPException(status_code=502, detail=str(e)) from e
