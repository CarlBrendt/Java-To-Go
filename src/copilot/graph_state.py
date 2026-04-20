from __future__ import annotations
from typing import TypedDict, List, Dict, Any


class MigrationGraphState(TypedDict, total=False):
    # ── Input ──
    java_project_path: str
    output_dir: str
    jar_path: str
    #: ID модели MWS (из GET /v1/models); если нет — берётся MODEL_NAME из .env
    mws_model_name: str | None

    # ── Stage 1: Analysis ──
    java_structure: Dict[str, Any]
    api_contract: List[Dict[str, Any]]
    dependency_graph: Dict[str, Any]
    package: str
    exception_handlers: List[Dict[str, Any]]

    # ── Stage 2: Planning ──
    migration_plan: str
    plan_steps: List[str]

    # ── Stage 3: Data Layer ──
    generated_models_code: str

    # ── Stage 4: Business Logic ──
    generated_service_code: str

    # ── Stage 5: API Layer ──
    generated_handlers_code: str

    # ── All generated code ──
    generated_go_code: Dict[str, str]

    # ── Stage 6: Verification ──
    verification_passed: bool
    verification_errors: List[str]
    manual_fixes: List[str]

    # ── Stage 7: Build Check ──
    build_passed: bool
    build_errors: List[str]
    build_fixes_applied: List[str]

    # ── Stage 8: Reporting ──
    report_generated: bool

    # ── Meta ──
    status: str
    current_node: str