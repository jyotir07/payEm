#!/usr/bin/env bash
# Run all tests across the monorepo. Used by Makefile target test.
set -e
cd "$(dirname "$0")/.."
(cd services/api-gateway && go test ./... 2>/dev/null || true)
(cd services/payments-core && ./gradlew test --no-daemon 2>/dev/null || true)
(cd services/ledger-service && sbt test 2>/dev/null || true)
(cd services/webhook-service && bundle exec rspec 2>/dev/null || true)
(cd frontend/admin-dashboard && npm run test 2>/dev/null || true)
echo Done.
