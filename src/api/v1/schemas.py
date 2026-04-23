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


class ValidationRunRequest(BaseModel):
    validation_run_id: str | None = None
    strategy_key: str | None = None
    mws_model: str | None = None


class ValidationRunCreateResponse(BaseModel):
    validation_run_id: str
    resolved_strategy_key: str
    status: str
    summary: str


class ValidationRunResponse(BaseModel):
    validation_run_id: str
    resolved_strategy_key: str
    status: str
    stage: str
    result: str | None = None
    parity_percent: int | None = None
    tests_total: int = 0
    tests_passed: int = 0
    tests_failed: int = 0
    summary: str
    migration_user_id: str | None = None
    started_at: str
    finished_at: str | None = None
