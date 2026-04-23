from langchain_openai import ChatOpenAI

from src.settings.config import APISettings


def resolve_mws_model_name(settings:APISettings, override:str|None)->str:
    if override is not None and str(override).strip():
        return str(override).strip()
    return settings.model_name


def build_mws_chat_llm(
    settings:APISettings,
    model_name:str|None=None,
)->ChatOpenAI:
    base=settings.resolve_mws_base_url()
    if not base:
        raise ValueError(
            "MWS GPT: задайте MWS_GPT_BASE_URL (например https://api.gpt.mws.ru/v1)"
        )
    if not (settings.model_api_key or "").strip():
        raise ValueError("MWS GPT: задайте MODEL_API_KEY (ключ sk-...)")

    resolved=resolve_mws_model_name(settings, model_name)
    kwargs:dict={
        "model":resolved,
        "api_key":settings.model_api_key,
        "base_url":base,
        "temperature":settings.temperature,
        "timeout":settings.mws_gpt_timeout_sec,
    }

    return ChatOpenAI(**kwargs)

if __name__ == "__main__":
    llm=build_mws_chat_llm(APISettings())
    print(llm.invoke("1+1"))