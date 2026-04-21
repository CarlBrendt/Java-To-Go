# Validation System

## Why This Document Exists

This document is intended to serve as persistent project context for future work on the repository.

Its purpose is twofold:

- describe the current Java-to-Go migration service in enough detail that future discussions do not require re-reading the whole repository;
- define the high-level meaning and direction of the planned validation system.

This is the main architectural context document for validation-related work.

Separate implementation stages are tracked in:

- [TASK_1.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_1.md)
- [TASK_2.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_2.md)
- [TASK_3.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_3.md)

## Product Purpose

The repository contains a service that performs semi-automatic migration of Java Spring Boot microservices into Go services based on Gin.

The product promise is not just "generate some Go code", but:

- analyze an input Java service;
- extract its API structure;
- generate a Go draft that preserves the contract as closely as possible;
- verify the generated result;
- build the generated service;
- produce reports that explain what was migrated, what failed, and what still requires manual work.

At the moment, the current system already supports upload, migration, packaging, and reporting. The missing strategic layer is a dedicated validation subsystem that measures parity between reference Java services and generated Go services.

## Current Service Overview

### What The Service Does Today

The service accepts a ZIP archive containing one or more Java projects, stores and extracts it in MinIO, runs a migration pipeline, packages the generated Go output, and returns a ready ZIP archive with reports.

The current flow is:

1. User uploads a ZIP archive through the frontend or API.
2. Backend stores the original ZIP in MinIO under `raw/`.
3. Backend extracts files to MinIO under `processed/`.
4. Backend downloads project files to a temporary local workspace.
5. Backend discovers Java project roots.
6. Backend runs the migration pipeline for each detected Java project.
7. Backend writes generated Go files and reports to a local output directory.
8. Backend packages all generated projects into one ZIP.
9. Backend uploads that ZIP to MinIO under `ready/`.
10. User downloads the result through API or frontend.

### What The Service Does Not Yet Do

It does not yet provide a dedicated validation workflow that:

- starts reference Java services from `lowcode/`;
- generates Go candidates for those references;
- starts generated Go services in a reproducible environment;
- runs parity tests against both sides;
- reports a formal pass/fail/parity result.

That planned subsystem is the subject of this document and the related task files.

## Repository Structure

### Top-Level Layout

- [main.py](/Users/macbook/PycharmProjects/Java-To-Go/main.py)
  FastAPI application entry point and router registration.

- [src/](/Users/macbook/PycharmProjects/Java-To-Go/src)
  Main backend code.

- [frontend/](/Users/macbook/PycharmProjects/Java-To-Go/frontend)
  React/Vite UI used to upload archives and download generated output.

- [java-tools/](/Users/macbook/PycharmProjects/Java-To-Go/java-tools)
  JavaParser-based tooling used for deterministic Java AST analysis.

- [lowcode/](/Users/macbook/PycharmProjects/Java-To-Go/lowcode)
  Reference Java projects. These are the key inputs for the future validation system.

- [docker-compose.yml](/Users/macbook/PycharmProjects/Java-To-Go/docker-compose.yml)
  Local container environment for frontend, backend, and MinIO.

- [Dockerfile](/Users/macbook/PycharmProjects/Java-To-Go/Dockerfile)
  Backend image definition. Includes Python, Java, and Go toolchains.

- [Makefile](/Users/macbook/PycharmProjects/Java-To-Go/Makefile)
  Common local commands for starting and stopping the environment.

- [run_migration.py](/Users/macbook/PycharmProjects/Java-To-Go/run_migration.py)
  Local script for invoking migration logic.

- [README.md](/Users/macbook/PycharmProjects/Java-To-Go/README.md)
  Project overview and basic usage notes.

### Backend Structure

- [src/api/v1/](/Users/macbook/PycharmProjects/Java-To-Go/src/api/v1)
  FastAPI routers, schemas, and task orchestration helpers.

- [src/services/](/Users/macbook/PycharmProjects/Java-To-Go/src/services)
  Infrastructure services such as MinIO integration and LLM client construction.

- [src/copilot/](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot)
  Migration pipeline implemented as a LangGraph workflow.

- [src/settings/](/Users/macbook/PycharmProjects/Java-To-Go/src/settings)
  Environment-driven configuration.

### Frontend Structure

- [frontend/src/App.jsx](/Users/macbook/PycharmProjects/Java-To-Go/frontend/src/App.jsx)
  Root page component.

- [frontend/src/components/ServiceDescription.jsx](/Users/macbook/PycharmProjects/Java-To-Go/frontend/src/components/ServiceDescription.jsx)
  Product description shown on the landing page.

- [frontend/src/components/MigrationUploadSection.jsx](/Users/macbook/PycharmProjects/Java-To-Go/frontend/src/components/MigrationUploadSection.jsx)
  Main upload UI for current migration flow.

## Runtime Architecture

### Current Running Components

The current docker-compose environment defines four services:

- `frontend`
  Nginx-served React frontend on port `3000`.

- `server`
  FastAPI backend on port `8585`.

- `minio`
  Object storage used for uploaded ZIPs, extracted project files, and generated result archives.

- `minio-init`
  One-time bucket initialization helper.

### Backend Runtime Dependencies

The backend image includes:

- Python 3.13;
- Java runtime for JavaParser tooling;
- Go toolchain for generated project build checks;
- Git and curl;
- project Python dependencies from [pyproject.toml](/Users/macbook/PycharmProjects/Java-To-Go/pyproject.toml).

This matters because the backend is not just an API layer. It is already an execution environment for parsing Java code, invoking LLM-driven generation, and performing Go build checks.

### Persistent and Temporary Storage

Current storage model:

- MinIO bucket stores uploaded input and packaged output;
- temporary local directories are used during migration runs;
- generated reports are written into output directories before packaging.

Current MinIO layout:

- `raw/user_<id>_<filename>.zip`
- `processed/user_<id>/...`
- `ready/user_<id>/<zip>`

## Current API Surface

The FastAPI app is created in [main.py](/Users/macbook/PycharmProjects/Java-To-Go/main.py) and exposes three router groups:

- main router under `/api/v1/java-to-go`
- MinIO/migration router under `/api/v1/minio`
- health router under `/api/v1`

### Health API

Defined in [src/api/v1/health_router.py](/Users/macbook/PycharmProjects/Java-To-Go/src/api/v1/health_router.py).

- `GET /api/v1/ping_health`

Purpose:

- simple liveness check for the backend.

### Main API

Defined in [src/api/v1/main_router.py](/Users/macbook/PycharmProjects/Java-To-Go/src/api/v1/main_router.py).

- `POST /api/v1/java-to-go/ping-agent`

Purpose:

- placeholder/test endpoint.

This route does not currently participate in the migration or validation workflow.

### Migration and Storage API

Defined in [src/api/v1/minio_router.py](/Users/macbook/PycharmProjects/Java-To-Go/src/api/v1/minio_router.py).

- `POST /api/v1/minio/minio-upload-zip?user_id=<id>&auto_migrate=<bool>`
  Upload ZIP archive and optionally start migration immediately.

- `POST /api/v1/minio/migrate?user_id=<id>`
  Start migration for already uploaded files.

- `GET /api/v1/minio/migrate/status?user_id=<id>`
  Check whether generated output is ready.

- `GET /api/v1/minio/minio-download-ready-zip?user_id=<id>&filename=<optional>`
  Download packaged generated Go result.

- `DELETE /api/v1/minio/minio-delete?object_name=<path>`
  Delete one object from MinIO.

- `DELETE /api/v1/minio/minio-delete-user?user_id=<id>`
  Delete all stored data for one user.

### Current API Characteristics

- current long-running work is triggered through background tasks;
- there is no first-class job model yet;
- there is no validation API yet;
- there is no diagnostics API yet.

## Current Migration Pipeline

### Pipeline Entry

The migration orchestration entrypoint is [src/copilot/run.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/run.py).

It builds and executes a LangGraph workflow defined in [src/copilot/graph.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/graph.py).

### Current Graph Stages

The current migration graph is:

1. `parse`
2. `plan`
3. `data_layer`
4. `business_logic`
5. `api_layer`
6. `verify`
7. `build_check`
8. `report`

### Stage Meanings

`parse`
Deterministic Java AST analysis using JavaParser tooling.

`plan`
LLM-generated migration plan based on parsed Java structure and API contract.

`data_layer`
LLM-generated Go DTO/model layer.

`business_logic`
LLM-generated Go service and repository code.

`api_layer`
Hybrid generation of router, main entrypoint, and handlers.

`verify`
Static checks against expected endpoints, model types, package declarations, and other basic invariants.

`build_check`
Applies deterministic fixes, writes files, runs Go build commands, and emits build diagnostics.

`report`
Creates structured reports and saves generated output.

### Key Supporting Files

- [src/copilot/nodes/analysis_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/analysis_node.py)
  Java AST scanning and contract extraction.

- [src/copilot/nodes/planning_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/planning_node.py)
  High-level migration plan generation.

- [src/copilot/nodes/data_layer_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/data_layer_node.py)
  DTO/model conversion.

- [src/copilot/nodes/business_logic_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/business_logic_node.py)
  Service/repository migration.

- [src/copilot/nodes/generation_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/generation_node.py)
  Router/main generation and handler generation.

- [src/copilot/nodes/verification_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/verification_node.py)
  Static parity-oriented checks inside the generated project.

- [src/copilot/nodes/build_check_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/build_check_node.py)
  Build verification and auto-fix logic.

- [src/copilot/nodes/reporting_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/reporting_node.py)
  Report generation and packaging.

### Important Architectural Observation

The existing pipeline already contains partial "validation-like" behavior:

- endpoint presence checks;
- model/type presence checks;
- build checks;
- startup checks for generated Go services;
- generated reports.

However, this is still internal validation of the generated output. It is not yet end-to-end behavioral comparison against running reference Java services.

## LLM and Parsing Model

### Deterministic Parsing

The repository uses JavaParser-based analysis through [java-tools/](/Users/macbook/PycharmProjects/Java-To-Go/java-tools) and [src/copilot/nodes/analysis_node.py](/Users/macbook/PycharmProjects/Java-To-Go/src/copilot/nodes/analysis_node.py).

This stage is intended to be deterministic and extract:

- controllers;
- DTOs;
- services;
- repositories;
- exception handlers;
- API contracts;
- dependency relationships.

### LLM-Driven Generation

The repository uses an MWS/OpenAI-compatible chat model configured through [src/services/mws_llm_factory.py](/Users/macbook/PycharmProjects/Java-To-Go/src/services/mws_llm_factory.py).

LLM usage currently covers:

- migration planning;
- DTO/model generation;
- business logic generation;
- handler generation.

This means that model choice can materially affect output quality. That is one of the main reasons a stable validation system is required.

## Frontend Today

The current frontend is intentionally narrow.

It currently supports:

- explaining the service purpose;
- selecting a ZIP archive;
- uploading the archive;
- polling migration status;
- downloading the generated result;
- deleting temporary user data when a run is canceled.

It does not yet support:

- validation runs;
- diagnostics dashboards;
- run history;
- comparison of model outputs;
- admin workflows;
- test suite management.

## Reference Projects In `lowcode/`

The `lowcode/` directory contains reference Java projects such as:

- `db-file-storage`
- `workflow-bridge`
- `workflow-db-call`
- `workflow-engine`
- `workflow-executor`
- `workflow-ftp`
- `workflow-ibmmq`
- `workflow-kafka`
- `workflow-mail`
- `workflow-odata`
- `workflow-primitives`
- `workflow-rabbitmq`
- `workflow-s3`
- `workflow-scheduler`
- `workflow-script-executor`
- `workflow-xslt`

These are strategically important because they provide:

- realistic Java inputs for migration;
- concrete service families with different behavior types;
- the behavioral source of truth for future parity testing.

The validation system should be built around them rather than around synthetic toy examples.

## Why The Validation System Is Needed

The future validation subsystem exists to answer one product-quality question:

`How close is the generated Go service to the behavior of the reference Java service?`

This matters because:

- migration quality depends on the selected model and prompts;
- different project types may fail in different ways;
- internal build success is not enough to prove behavioral correctness;
- users need a fast quality signal after generation;
- engineers need deeper diagnostics to improve the migration pipeline.

Without a dedicated validation system, output quality remains partially opaque.

## Planned Validation System: High-Level Meaning

The planned validation system is a separate quality subsystem layered on top of the existing migration engine.

Its role is to:

- execute reference validation runs against projects from `lowcode/`;
- generate Go candidates using the existing migration pipeline;
- use MinIO as the artifact handoff point for generated Go ZIP archives;
- start both Java reference and generated Go services in controlled containers;
- run parity tests against both sides;
- compare results after normalization;
- report a pass/fail/partial result and parity percentage;
- preserve enough artifacts for diagnosis and regression analysis.

This system should be designed as backend-driven asynchronous orchestration, not as a frontend-driven script runner.

### Runtime Roles

The future validation architecture should be split into three clear runtime roles.

`Main backend`

- accepts the user action to start validation;
- tracks validation runs;
- invokes validation execution;
- returns validation status and summary to the frontend.

The main backend should not own knowledge about which concrete parity tests exist.

`Test orchestrator`

- owns knowledge about built-in reference services and suites;
- starts reference Java services;
- waits for or retrieves the generated Go ZIP from MinIO;
- builds and starts the Go candidate runtime;
- triggers the parity runner;
- aggregates structured reports into one validation result.

`Parity runner`

- executes black-box API tests against Java and Go;
- knows only the suite being run and the two base URLs;
- produces structured machine-readable reports.

## Validation Principles

- Reference Java service is the source of truth.
- Validation compares observable API behavior, not implementation details.
- Raw JSON equality is insufficient by itself.
- Response normalization is required for noisy fields.
- Validation failures must be classified, not collapsed into one generic error.
- Every validation run must produce structured artifacts.
- Docker console logs are not the business source of truth for validation results.
- Built-in quick validation should remain simple and stable.

## Planned Validation Flow

The intended future flow is:

1. User or admin starts a validation run.
2. Main backend creates a run record.
3. Main backend delegates execution to a validation orchestrator.
4. Orchestrator resolves the built-in reference project and suite for the run.
5. Orchestrator initiates or waits for Go generation through the existing migration pipeline.
6. Orchestrator retrieves the generated Go ZIP from MinIO.
7. Orchestrator builds the generated Go service.
8. Orchestrator starts the Java reference runtime.
9. Orchestrator starts the Go candidate runtime.
10. Orchestrator waits for health readiness.
11. Parity runner sends identical scenarios to Java and Go.
12. Responses are normalized and compared.
13. Parity runner writes structured reports.
14. Orchestrator aggregates those reports and returns the validation result to the main backend.
15. The main backend exposes the final status and summary to the frontend.

## Validation Artifact Handoff

For the planned backend-driven flow, MinIO is the handoff point between migration and validation.

Current generated output contract:

- bucket: `java-to-go`
- raw upload prefix: `raw/`
- extracted source prefix: `processed/`
- generated ZIP prefix: `ready/`

The generated Go ZIP currently follows the pattern:

- `ready/user_<user_id>/user_<user_id>.zip`

This artifact path is the current source of truth for the Go candidate that validation must build and run.

## Failure Taxonomy

Failures must be split into distinct classes:

- `Infrastructure failure`
  Docker image or container startup problems, healthcheck failures, environment issues.

- `Generation failure`
  Migration pipeline failed or generated incomplete output.

- `Build failure`
  Generated Go project does not compile or package correctly.

- `Behavior mismatch`
  Java and Go differ at the API behavior level.

- `Validation system failure`
  Test harness, suite packaging, fixtures, or orchestrator issues.

This classification is required so engineers do not confuse test infrastructure problems with migration defects.

## User and Admin Separation

The planned validation UX must be role-aware.

### User View

The regular user should get:

- one simple action such as "Check system";
- a short quick validation;
- simple status stages;
- a final result like `passed`, `partial`, or `failed`;
- parity percentage;
- a minimal summary without raw logs.

### Admin View

The admin should get:

- the same built-in quick validation;
- detailed diagnostics;
- run history;
- access to artifacts;
- mismatch details by endpoint or test case;
- logs and stage information;
- control over additional test suites.

## Built-In Versus Custom Validation Suites

The system should eventually support three categories of validation suites:

### Built-In Base Quick Suite

- used by user and admin;
- fixed and stable;
- short and representative;
- intended for fast confidence checks.

### Built-In Base Full Suite

- admin-facing baseline deep check;
- larger than Quick Suite;
- still owned by the system.

### Custom Suites

- uploaded and managed by admin;
- optional;
- used only in diagnostics/admin mode;
- may target specific project types.

## Suggested Domain Concepts

The future design is expected to use conceptual entities such as:

- `ReferenceProject`
- `ProjectType`
- `ValidationRun`
- `ValidationReport`
- `ValidationArtifact`
- `TestSuite`
- `TestSuiteVersion`

These concepts are not yet fully implemented in code, but they are useful for structuring future work.

## Stable Invariants

These rules should remain stable unless deliberately changed:

- frontend never orchestrates Docker directly;
- validation is asynchronous backend work;
- `lowcode/` reference Java services are the source of truth;
- quick validation remains simple and fast;
- admin diagnostics remains deeper and more configurable;
- custom suites are admin-only;
- every run stores artifacts and reports;
- parity is measured at the API behavior level.

## Task Breakdown

The validation initiative is intentionally split into three implementation stages.

### Stage 1

Basic standard validation run with a simple result:

- run or fail;
- parity percentage;
- no deep role separation yet.

Detailed scope is defined in [TASK_1.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_1.md).

### Stage 2

Split the experience between user and admin:

- simple quick result for user;
- detailed diagnostics for admin.

Detailed scope is defined in [TASK_2.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_2.md).

### Stage 3

Allow admin to add custom validation suites:

- upload new tests;
- manage them;
- bind them to project types or projects.

Detailed scope is defined in [TASK_3.md](/Users/macbook/PycharmProjects/Java-To-Go/docs/TASK_3.md).

## Recommended Next Use Of This Document

When opening a new discussion about the project, this document should be used as the default architectural context for:

- what the current service already does;
- how the repository is structured;
- where current backend and frontend responsibilities live;
- what the validation task means at a high level;
- which phase of validation work is currently under discussion.
