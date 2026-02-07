#!/usr/bin/env bash
# Format code according to .editorconfig and language conventions. Used by Makefile target 'fmt'.
# Responsibility: Apply formatters (gofmt, scalafmt, prettier, etc.) across the monorepo.

set -e
cd "$(dirname "$0")/.."

echo "Formatting api-gateway..."
(cd services/api-gateway && gofmt -w . 2>/dev/null || true)

echo "Formatting payments-core..."
(cd services/payments-core && ./gradlew spotlessApply --no-daemon 2>/dev/null || true)

echo "Formatting ledger-service..."
(cd services/ledger-service && sbt scalafmt 2>/dev/null || true)

echo "Formatting webhook-service..."
(cd services/webhook-service && bundle exec rubocop -a 2>/dev/null || true)

echo "Formatting admin-dashboard..."
(cd frontend/admin-dashboard && npx prettier --write "src/**/*.{ts,tsx,js,jsx,css}" 2>/dev/null || true)

echo "Done."
