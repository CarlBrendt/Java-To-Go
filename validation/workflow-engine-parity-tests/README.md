# Workflow Engine Parity Tests

This module contains external black-box parity tests for `workflow-engine`.

The purpose of the suite is:

- send the same HTTP scenarios to the reference Java service;
- send the same HTTP scenarios to the generated Go service;
- compare status codes and normalized response bodies;
- report parity at the API level.

## Current Scope

This is the first scaffold for the parity-test MVP.

At this stage the module provides:

- a standalone Maven test project;
- direct JUnit parity tests for selected `workflow-engine` cases;
- request payloads stored in `src/test/resources/TestData`;
- an HTTP-based runner that executes the same requests against Java and Go services.

## Planned Next Steps

1. Add richer response normalization rules.
2. Add richer mismatch reporting.
3. Add Docker-based execution for the reference Java service, generated Go service, and the test runner.

## Endpoint Values

Default docker-oriented values:

- `parity.enabled=false` by default
- `parity.java.base-url=http://workflow-engine-java:9090`
- `parity.go.base-url=http://workflow-engine-go:8080`

You can override them when needed.

Example local override:

- `parity.java.base-url`
- `parity.go.base-url`

Example:

```bash
mvn -Dmaven.repo.local=.m2 \
  -Dparity.enabled=true \
  -Dparity.java.base-url=http://localhost:9090 \
  -Dparity.go.base-url=http://localhost:8080 \
  test
```

## Docker

This module includes:

- [Dockerfile](/Users/macbook/PycharmProjects/Java-To-Go/validation/workflow-engine-parity-tests/Dockerfile)
- [docker-compose.yml](/Users/macbook/PycharmProjects/Java-To-Go/validation/workflow-engine-parity-tests/docker-compose.yml)
- [validation-stack/docker-compose.yml](/Users/macbook/PycharmProjects/Java-To-Go/validation/workflow-engine-parity-tests/validation-stack/docker-compose.yml)

The compose file containerizes the parity test runner itself. It expects `workflow-engine-java` and `workflow-engine-go` to be reachable on the same Docker network.

The full validation runtime is managed separately from the main app runtime. Use the root `Makefile` commands such as `make validation-up`, `make validation-up-java`, and `make validation-test`.

If you run tests from IDE with default values, they are skipped.

If you run them in Docker with `parity.enabled=true` but without reachable target services, they fail with connection errors.

## Test Data Sources

The initial payloads are based on the manual files in:

- `/Users/macbook/PycharmProjects/Java-To-Go/lowcode/Набор тест-кейсов`
