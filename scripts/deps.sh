#!/usr/bin/env bash
# Install dependencies for all services and frontend. Used by Makefile target deps.
set -e
cd "$(dirname "$0")/.."
(cd services/api-gateway && go mod download)
(cd services/payments-core && ./gradlew dependencies --no-daemon)
(cd services/ledger-service && sbt compile)
(cd services/webhook-service && bundle install)
(cd frontend/admin-dashboard && npm install)
echo Done.
