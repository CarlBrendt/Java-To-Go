from langchain_openai import ChatOpenAI

from src.settings.config import APISettings

def build_mws_chat_llm(settings:APISettings)->ChatOpenAI:
    base=settings.resolve_mws_base_url()
    if not base:
        raise ValueError(
            "MWS GPT: задайте MWS_GPT_BASE_URL (например https://api.gpt.mws.ru/v1)"
        )
    if not (settings.model_api_key or "").strip():
        raise ValueError("MWS GPT: задайте MODEL_API_KEY (ключ sk-...)")

    kwargs:dict={
        "model":settings.model_name,
        "api_key":settings.model_api_key,
        "base_url":base,
        "temperature":settings.temperature,
        "timeout":settings.mws_gpt_timeout_sec,
    }
    if settings.mws_gpt_max_tokens is not None:
        kwargs["max_tokens"]=settings.mws_gpt_max_tokens

    return ChatOpenAI(**kwargs)

if __name__ == "__main__":
    
    llm = build_mws_chat_llm(APISettings())
    print(llm.invoke("1+1"))