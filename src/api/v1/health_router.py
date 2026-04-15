import logging
from fastapi import APIRouter

from src.api.v1.schemas import HealthResponse

logger = logging.getLogger(__name__)
router = APIRouter()

@router.get("/ping_health")
async def check_health() -> HealthResponse:

    return HealthResponse(is_alive=True, health_message="Server is ready to respond")

        