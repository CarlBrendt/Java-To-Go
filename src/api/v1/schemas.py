from enum import Enum
from typing import Optional

from pydantic import BaseModel, field_validator


# Просто примерный draft
class TaskResponse(BaseModel):
    task_id: int
    task_result: str

class TaskRequest(BaseModel):
    zip: str

class HealthResponse(BaseModel):
    is_alive: bool
    health_message: str
