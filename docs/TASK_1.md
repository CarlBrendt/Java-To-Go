# TASK 1

## Title

Base validation run with simple outcome and parity percentage.

## Goal

Implement the first usable version of the validation system that can answer a simple product question:

`Did the generated Go service pass the standard validation run, and what parity percentage did it achieve?`

This stage is intentionally minimal. It should produce one standard validation result without introducing user/admin separation or custom suite management yet.

## Problem This Stage Solves

Today the system can generate Go code and produce internal migration reports, but it cannot formally validate behavioral parity against reference Java services from `lowcode/`.

Stage 1 introduces the first end-to-end validation loop:

- select a reference project;
- generate Go code;
- build and run the Java and Go services;
- execute a standard parity test set;
- calculate a simple result.

## Scope

Stage 1 should include:

- one standard validation flow;
- one built-in standard suite;
- one result summary;
- one parity percentage;
- one basic report;
- asynchronous backend execution.

Stage 1 should not yet include:

- separate user and admin views;
- diagnostics dashboard;
- custom suite uploads;
- suite management UI.

## Expected User-Facing Output

The result of a run should be understandable in a few seconds.

Expected output:

- `passed`, `partial`, or `failed`;
- parity percentage;
- total number of checked scenarios;
- count of failed scenarios;
- short human-readable summary.

## High-Level Flow

1. Start validation run.
2. Main backend delegates execution to a dedicated test orchestrator.
3. Orchestrator starts the reference Java runtime for the built-in validation scenario.
4. Orchestrator waits for the generated Go ZIP in MinIO.
5. Orchestrator downloads the ZIP, extracts it, builds the Go candidate image, and starts the Go service.
6. Orchestrator performs health checks for Java and Go.
7. Orchestrator runs the built-in parity suite through the parity runner.
8. Parity runner writes structured reports.
9. Orchestrator aggregates the reports and returns the result to the main backend.
10. Main backend returns the final status and summary to the frontend.

## Responsibility Boundaries

### Main Backend

The main Python backend should:

- accept the user action to start validation;
- create and track the validation run record;
- invoke the external validation orchestrator;
- receive the final validation result;
- expose a simple API for frontend polling.

The main backend should not:

- know which concrete parity tests exist;
- know which reference service or suite was selected internally;
- parse raw Docker logs as the business source of truth.

### Test Orchestrator

The orchestrator is the owner of validation execution logic.

It should:

- know which built-in reference services and suites are available;
- start the reference Java runtime;
- wait for or request the generated Go artifact;
- download the generated ZIP from MinIO;
- build and start the Go candidate runtime;
- run health checks;
- trigger the parity runner;
- aggregate structured test reports into one validation result.

### Parity Runner

The parity runner should:

- execute black-box HTTP parity tests against Java and Go;
- know only the suite it must run and the two base URLs;
- write structured machine-readable reports;
- avoid making Docker console output the primary result source.

## Artifact Contract

For Stage 1, the generated Go service artifact is taken from MinIO.

Source of truth:

- bucket: `java-to-go`
- current generated ZIP path pattern: `ready/user_<user_id>/user_<user_id>.zip`

This means Stage 1 validation should treat MinIO as the handoff point between:

- the existing migration pipeline;
- the new validation pipeline.

Reference Java services and parity runner code remain in the git repository as source code. Built Docker images are not stored in git.

## Validation Result Contract

Stage 1 validation should return a compact backend-friendly result.

Minimum fields:

- `validation_run_id`
- `status`
- `result`
- `parity_percent`
- `tests_total`
- `tests_passed`
- `tests_failed`
- `summary`

Recommended meaning:

- `status` describes job state such as `queued`, `running`, or `finished`;
- `result` describes validation outcome such as `passed`, `partial`, or `failed`;
- `summary` is a short human-readable conclusion suitable for the frontend quick result.

## Report Source

The validation result should not be derived from raw Docker console logs.

Instead:

- the parity runner should produce structured JSON reports;
- the orchestrator should accumulate those reports;
- the main backend should map the aggregated result into API DTOs.

For Stage 1, Docker remains only the execution environment for containers and networking.

## Core Deliverables

- backend validation run model;
- backend endpoint to start a validation run;
- backend endpoint to read validation status;
- backend endpoint to read the result summary;
- run workspace isolation;
- built-in standard parity suite runner;
- summary report generation.

## Suggested Minimal Status Model

- `queued`
- `preparing`
- `generating_go`
- `building_services`
- `starting_services`
- `running_tests`
- `collecting_report`
- `done`
- `failed`

## Suggested Acceptance Criteria

- at least one reference project can be validated end-to-end;
- the system reports a final status;
- the system reports parity percentage;
- failure type is distinguishable from success;
- artifacts are stored for the run;
- the flow does not require manual inspection of raw logs to understand the main result.

## Risks

- Docker orchestration may be the first practical bottleneck;
- Java and Go services may require different readiness checks;
- parity comparison may produce noise without early normalization rules;
- if the suite is too broad in Stage 1, delivery will slow down.

## Recommendation

Keep the first suite intentionally small and stable. The primary target of Stage 1 is to prove the end-to-end validation loop, not to achieve exhaustive coverage.

## Current Implementation State

This section captures the current repository state so the task can be continued without relying on chat history.

### Implemented So Far

- A dedicated Java Spring Boot validation orchestrator exists in `validation/validation-orchestrator`.
- A Java/JUnit black-box parity test module exists in `validation/workflow-engine-parity-tests`.
- The main backend exposes validation proxy endpoints:
  - `POST /api/v1/validation/runs`
  - `GET /api/v1/validation/runs/{validation_run_id}`
- The frontend has a bottom validation card with:
  - a `Start` button;
  - current status;
  - current stage;
  - overall parity percentage;
  - passed/total test count;
  - human-readable summary or error.
- The root `docker-compose.yml` includes `validation-orchestrator` together with the main backend, frontend, and MinIO.
- The orchestrator has a strategy registry with the first built-in strategy: `workflow-engine`.
- The `workflow-engine` strategy currently:
  - takes Java reference source from `lowcode/workflow-engine`;
  - archives it into an isolated validation workspace;
  - uploads the archive to the main backend with `auto_migrate=true`;
  - waits for the generated Go artifact;
  - downloads the generated Go ZIP;
  - extracts it;
  - builds a Docker image for the generated Go service;
  - starts the generated Go service in the validation Docker network;
  - starts the Java reference service;
  - runs the parity test container.
- Validation start is idempotent at the orchestrator level: if a run is already queued or running, another start request should return the active run instead of starting a parallel one.
- Frontend interaction rule:
  - while validation is running, manual migration and archive editing are blocked;
  - manual migration does not block starting validation, because validation uses its own `lowcode` reference source.

### Current Runtime State

The end-to-end pipeline is wired, but the first successful validation run is not complete yet.

The validation work has been cleaned up and saved in local commit:

- `9339fc5 Add workflow engine validation flow`

Local verification after cleanup:

- `validation/workflow-engine-parity-tests`: `mvn -Dmaven.repo.local=.m2 test` passed.
- `validation/validation-orchestrator`: `mvn -Dmaven.repo.local=.m2 test` passed.
- `git diff --cached --check` passed before the local commit.

The latest observed validation run failed before parity tests actually executed:

- status: `failed`;
- stage: `failed`;
- tests total: `0`;
- summary: `Validation execution crashed: request timed out`.

This means the failure happened while the orchestrator was waiting for or communicating with the main backend during Go generation. It did not yet reach the real parity test execution stage.

Important update: this timeout may already be fixed by newer main backend/frontend changes in `dev`. The next step is to merge `dev` first, not to blindly rewrite the orchestrator timeout logic.

Current blocker for merge:

- local git cannot fetch `origin/dev` because GitHub credentials are not available in this environment;
- `git fetch origin dev` currently fails with `fatal: could not read Username for 'https://github.com': Device not configured`.

Manual command needed before continuing:

```bash
git fetch origin dev:refs/remotes/origin/dev
```

### Known Gaps Before Acceptance

- Merge the latest `dev` changes for the main backend and frontend into `feature/workflow-engine-parity-tests`.
- Resolve conflicts carefully, especially in:
  - `docker-compose.yml`;
  - `frontend/src/components/MigrationUploadSection.jsx`;
  - `frontend/src/App.scss`;
  - `main.py`;
  - `src/api/v1/schemas.py`;
  - `src/settings/config.py`;
  - `pyproject.toml`.
- Re-check whether the orchestrator still needs custom polling timeout handling after `dev` is merged.
- ZIP extraction in the migration flow should ignore macOS metadata such as `__MACOSX` and `._*`, if this is not already fixed in `dev`.
- The parity runner should produce structured JSON reports; currently the orchestrator still derives the summary from Maven/JUnit console output.
- The first complete end-to-end run must still be verified with `tests_total > 0` and a real `parity_percent`.
- If generated Go code does not build or start, the validation result should still return a clear frontend-friendly failure summary.

### Next Suggested Work

1. Fetch `origin/dev` with working GitHub credentials.
2. Merge `origin/dev` into `feature/workflow-engine-parity-tests`.
3. Resolve frontend/backend/docker-compose conflicts without dropping the validation orchestrator wiring.
4. Re-run Java checks:
   - `cd validation/workflow-engine-parity-tests && mvn -Dmaven.repo.local=.m2 test`
   - `cd validation/validation-orchestrator && mvn -Dmaven.repo.local=.m2 test`
5. Rebuild and rerun the full Docker Compose stack.
6. Start validation from the frontend and confirm that the run reaches either:
   - Go build/runtime failure with a clear summary; or
   - parity test execution with non-zero test counts.
7. Replace Maven log parsing with structured JSON report aggregation.
