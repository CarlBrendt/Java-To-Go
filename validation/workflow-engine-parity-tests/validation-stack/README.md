# Validation Stack

This stack is the first local validation environment for `workflow-engine` parity testing.

The target topology is:

- `postgresql-wf`
- `temporal`
- `workflow-engine-java`
- `parity-tests`

At this stage:

- `workflow-engine-java` is the fixed reference Java service;
- `parity-tests` runs against `workflow-engine-java` and later against `workflow-engine-go`;
- `workflow-engine-go` is not added yet.

The service names are intentionally stable because parity tests are bound to them:

- `http://workflow-engine-java:9090`
- `http://workflow-engine-go:8080`

## Current Scope

This stack is a bootstrap environment for the reference Java side only.

The first goal is:

1. build and run `workflow-engine-java`;
2. confirm it starts with PostgreSQL and Temporal;
3. then add Go candidate and real parity comparison.

## Usage

This stack is intentionally separate from the main project `docker-compose.yml`.

Use the root `Makefile` commands:

- `make validation-build`
- `make validation-up`
- `make validation-up-java`
- `make validation-test`
- `make validation-down`
- `make validation-logs`
- `make validation-logs-java`

Direct compose path if needed:

```bash
docker compose -f validation/workflow-engine-parity-tests/validation-stack/docker-compose.yml up
```
