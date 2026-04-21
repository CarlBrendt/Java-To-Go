# validation-orchestrator

Spring Boot 3 / Java 17 service that starts validation runs for the existing `workflow-engine` parity stack.

Current scope:

- accepts validation run requests over HTTP;
- resolves the validation strategy through an internal strategy registry;
- starts the reference Java runtime from `validation/workflow-engine-parity-tests/validation-stack`;
- runs the built-in parity tests;
- keeps run state in memory;
- exposes run status over HTTP.

Current built-in strategy:

- `workflow-engine`

Current limitation:

- it does not yet fetch the generated Go ZIP from MinIO or build `workflow-engine-go`;
- until that integration is added, parity runs are expected to fail with `Go service is unreachable ...`.

## Endpoints

- `GET /api/v1/orchestrator/health`
- `POST /api/v1/orchestrator/runs`
- `GET /api/v1/orchestrator/runs/{runId}`

## Request example

```json
{
  "validationRunId": "val_001",
  "migrationUserId": "user_1776613082338"
}
```

`migrationUserId` is stored for the future MinIO integration but is not used yet.

## Run locally

```bash
cd validation/validation-orchestrator
mvn package
java -jar target/validation-orchestrator-0.1.0-SNAPSHOT.jar
```

Spring properties:

```bash
java \
  -Dserver.port=8095 \
  -jar target/validation-orchestrator-0.1.0-SNAPSHOT.jar \
  --orchestrator.repo-root=/Users/macbook/PycharmProjects/Java-To-Go
```

Or through Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--orchestrator.repo-root=/Users/macbook/PycharmProjects/Java-To-Go"
```
