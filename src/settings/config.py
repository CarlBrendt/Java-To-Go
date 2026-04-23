from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class APISettings(BaseSettings):
    model_config=SettingsConfigDict(
        env_file=Path("./.env"),
        env_file_encoding="utf-8",
        extra="allow",
    )

    host:str
    port:int
    secret_key:str
    model_api:str="placeholder"
    model_api_key:str
    model_name:str="gpt-oss-120b"
    temperature:float=0.0

    mws_gpt_base_url:str
    mws_gpt_timeout_sec:float=120.0

    minio_endpoint:str
    minio_access_key:str
    minio_secret_key:str
    minio_secure:bool
    minio_bucket:str="java-to-go"
    validation_orchestrator_base_url:str="http://validation-orchestrator:8095"

    # Явный allowlist id из каталога MWS (снимок ответа /v1/models). Обновляйте при появлении новых моделей.
    migration_llm_allowlist: frozenset[str] = frozenset(
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
        })


    def resolve_mws_base_url(self)->str|None:
        raw=(self.mws_gpt_base_url or "").strip()
        if raw.startswith("http"):
            return raw.rstrip("/")
        return None
