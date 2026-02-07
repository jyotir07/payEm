# payments-platform — Common developer commands.
# Use this Makefile for formatting, linting, tests, and local orchestration.
# See README.md for onboarding and usage.

.PHONY: deps up down test lint fmt clean

# Install dependencies for all services and frontend.
deps:
	$(MAKE) -C scripts deps

# Start all services and dependencies via Docker Compose (local dev).
up:
	docker-compose up -d

# Stop all containers.
down:
	docker-compose down

# Run all tests (unit + integration) across the monorepo.
test:
	$(MAKE) -C scripts test

# Run linters for all supported languages and frontend.
lint:
	$(MAKE) -C scripts lint

# Format code according to .editorconfig and language conventions.
fmt:
	$(MAKE) -C scripts fmt

# Remove build artifacts and caches.
clean:
	$(MAKE) -C scripts clean
