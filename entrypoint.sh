#!/bin/bash
# entrypoint.sh

mkdir -p /tmp/golangci-lint-cache
export GOLANGCI_LINT_CACHE=/tmp/golangci-lint-cache

exec python -m main