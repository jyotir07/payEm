# payments-platform

Production-grade monorepo for a distributed payments platform.

## Repository overview

This repository contains:

- **services/** — Backend microservices (API Gateway, Payments Core, Ledger, Webhook)
- **frontend/** — Admin dashboard for payment lifecycle and operational visibility
- **libs/** — Shared libraries, contracts (proto/OpenAPI), auth, idempotency, observability
- **infra/** — Docker, Kubernetes, and Terraform for deployment
- **docs/** — Architecture, payment lifecycle, ledger invariants, production-readiness
- **scripts/** — Formatting, linting, and test runner helpers
- **.github/** — CI workflows (GitHub Actions)

## Prerequisites

- Go 1.21+
- Java 17+ (for payments-core)
- Scala 2.13+ / sbt (for ledger-service)
- Ruby 3.x (for webhook-service)
- Node.js 18+ (for frontend)
- Docker and Docker Compose (for local orchestration)

## Quick start

```bash
# Install dependencies and run all services locally
make deps
make up

# Run tests
make test

# Lint and format
make lint
make fmt
```

See [docs/architecture.md](docs/architecture.md) for system design and [docs/production-readiness.md](docs/production-readiness.md) for deployment practices.

## Onboarding

1. Read this README and `docs/architecture.md`.
2. Review `docs/payment-lifecycle.md` and `docs/ledger-invariants.md` for domain context.
3. Run `make up` and verify services start.
4. Each service has its own README under `services/<service-name>/`.

## Contributing

- One logical change per PR; keep reviews scoped.
- Run `make lint` and `make test` before pushing.
- Follow `.editorconfig` for consistent formatting.
