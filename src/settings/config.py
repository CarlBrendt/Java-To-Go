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
    model_name:str="mws-gpt-alpha"
    temperature:float=0.0

    mws_gpt_base_url:str
    mws_gpt_timeout_sec:float=120.0
    mws_gpt_max_tokens:int|None=4096

    minio_endpoint:str
    minio_access_key:str
    minio_secret_key:str
    minio_secure:bool
    minio_bucket:str="java-to-go"

    def resolve_mws_base_url(self)->str|None:
        raw=(self.mws_gpt_base_url or "").strip()
        if raw.startswith("http"):
            return raw.rstrip("/")
        return None
