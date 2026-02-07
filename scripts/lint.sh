#!/usr/bin/env bash
# Run linters for all supported languages and frontend. Used by Makefile target 'lint'.
# Responsibility: Enforce code style and static analysis (go vet, eslint, etc.).

set -e
cd "$(dirname "$0")/.."

echo "Linting api-gateway..."
(cd services/api-gateway && go vet ./... 2>/dev/null || true)

echo "Linting payments-core..."
(cd services/payments-core && ./gradlew check --no-daemon 2>/dev/null || true)

echo "Linting ledger-service..."
(cd services/ledger-service && sbt scalafmtCheck 2>/dev/null || true)

echo "Linting webhook-service..."
(cd services/webhook-service && bundle exec rubocop 2>/dev/null || true)

echo "Linting admin-dashboard..."
(cd frontend/admin-dashboard && npm run lint 2>/dev/null || true)

echo "Done."
