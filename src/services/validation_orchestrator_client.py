from __future__ import annotations

import httpx

from src.api.v1.schemas import (
    ValidationRunCreateResponse,
    ValidationRunRequest,
    ValidationRunResponse,
)


class ValidationOrchestratorClient:
    def __init__(self, base_url: str, timeout: float = 30.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    async def start_run(
        self,
        payload: ValidationRunRequest,
    ) -> ValidationRunCreateResponse:
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.post(
                f"{self._base_url}/api/v1/orchestrator/runs",
                json={
                    "validationRunId": payload.validation_run_id,
                    "strategyKey": payload.strategy_key,
                    "mwsModel": payload.mws_model,
                },
            )
            response.raise_for_status()
            data = response.json()
            return ValidationRunCreateResponse(
                validation_run_id=data["validationRunId"],
                resolved_strategy_key=data["resolvedStrategyKey"],
                status=data["status"],
                summary=data["summary"],
            )

    async def get_run(self, validation_run_id: str) -> ValidationRunResponse:
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.get(
                f"{self._base_url}/api/v1/orchestrator/runs/{validation_run_id}"
            )
            response.raise_for_status()
            data = response.json()
            return ValidationRunResponse(
                validation_run_id=data["validationRunId"],
                resolved_strategy_key=data["resolvedStrategyKey"],
                status=data["status"],
                stage=data["stage"],
                result=data.get("result"),
                parity_percent=data.get("parityPercent"),
                tests_total=data.get("testsTotal", 0),
                tests_passed=data.get("testsPassed", 0),
                tests_failed=data.get("testsFailed", 0),
                summary=data["summary"],
                migration_user_id=data.get("migrationUserId"),
                started_at=data["startedAt"],
                finished_at=data.get("finishedAt"),
            )
