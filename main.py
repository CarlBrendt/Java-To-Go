import logging
import uvicorn
from fastapi import FastAPI

from starlette.middleware.sessions import SessionMiddleware
from contextlib import asynccontextmanager


from src.settings.config import APISettings
from src.services.minio_service import MinioService
from src.api.v1.main_router import router as main_router
from src.api.v1.health_router import router as health_router
from src.api.v1.minio_router import router as minio_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[logging.StreamHandler()]
)

settings = APISettings()

minio_service_instance = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global minio_service_instance
    logging.info("Starting up...")
    
    try:
        minio_service_instance = MinioService()
        
        app.state.minio = minio_service_instance
        
        print("Minio connection established and registered in app.state")
        
    except Exception as e:
        logging.error(f"Failed to initialize application: {e}")
        raise  
    
    yield
    logging.info("Shutting down...")


app = FastAPI(
    title="Java-to-Go Copilot",
    docs_url="/api/openapi",
    openapi_url="/api/openapi.json",
    lifespan=lifespan
)

app.include_router(main_router, prefix="/api/v1/java-to-go", tags=["Java-to-Go Copilot"])
app.include_router(minio_router, prefix="/api/v1/minio", tags=["Minio"])
app.include_router(health_router, prefix="/api/v1", tags=["Health"])

app.add_middleware(SessionMiddleware, secret_key=settings.secret_key)

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        timeout_keep_alive=30,
        limit_concurrency=1000,
        log_level="info"
    )