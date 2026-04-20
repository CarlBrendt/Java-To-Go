"""MWS GPT: список моделей для UI и общие утилиты."""

from __future__ import annotations

import logging

import httpx
from fastapi import APIRouter, HTTPException

from src.settings.config import APISettings

logger = logging.getLogger(__name__)
router = APIRouter()
_settings = APISettings()

# Явный allowlist id из каталога MWS (снимок ответа /v1/models). Обновляйте при появлении новых моделей.
_MIGRATION_LLM_ALLOWLIST: frozenset[str] = frozenset(
    {
        "gemma-3-27b-it",
        "llama-3.1-8b-instruct",
        "gpt-oss-120b",
        "qwen2.5-32b-instruct",
        "qwen2.5-coder-7b-instruct",
        "glm-4.6-357b",
        "qwen2.5-vl",
        "qwq-32b",
        "qwen3-vl-30b-a3b-instruct",
        "kimi-k2-instruct",
        "mws-gpt-alpha",
        "qwen3-32b",
        "t-pro-it-1.0",
        "qwen2.5-72b-instruct",
        "cotype-pro-vl-32b",
        "llama-3.3-70b-instruct",
        "deepseek-r1-distill-qwen-32b",
        "qwen3-coder-480b-a35b",
        "gpt-oss-20b",
        "qwen3-235b-a22b-instruct-2507-fp8",
        "qwen2.5-vl-72b",
    }
)


def _is_suitable_migration_llm(model_id: str) -> bool:
    """Только id из _MIGRATION_LLM_ALLOWLIST (регистр не важен)."""
    s = model_id.strip().lower()
    return bool(s) and s in _MIGRATION_LLM_ALLOWLIST


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
