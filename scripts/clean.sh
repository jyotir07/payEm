#!/usr/bin/env bash
# Remove build artifacts and caches. Used by Makefile target clean.
set -e
cd "$(dirname "$0")/.."
(cd services/api-gateway && go clean -cache 2>/dev/null || true)
(cd services/payments-core && ./gradlew clean --no-daemon 2>/dev/null || true)
(cd services/ledger-service && sbt clean 2>/dev/null || true)
(cd frontend/admin-dashboard && rm -rf node_modules/.cache dist 2>/dev/null || true)
echo Done.
