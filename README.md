# Payments-platform

Production-grade monorepo for a distributed payments platform.

---

## Vision

This project simulates a production-grade distributed payments platform inspired by modern fintech infrastructure. It models real-world payment processing concepts such as:

- Idempotent API design
- Double-entry accounting
- Explicit payment lifecycle state machines
- Event-driven webhook delivery
- Service-to-service communication
- Observability and production readiness practices

The goal of this project is to demonstrate system design thinking, distributed architecture principles, and production-safe financial logic across multiple services and languages.

---

## High-Level Architecture

The platform consists of four core backend services:

| Service | Language | Responsibility |
|----------|----------|----------------|
| API Gateway | Go | External HTTP interface, authentication, idempotency enforcement, request validation |
| Payments Core | Java | Payment lifecycle state machine, orchestration, business logic |
| Ledger Service | Scala | Double-entry accounting, immutable journal, balance derivation |
| Webhook Service | Ruby | Event delivery system with signing, retries, and backoff logic |

### Architectural Principles

- Each service owns its database and schema
- Clear separation between transport, domain, and infrastructure layers
- Immutable ledger design for financial safety
- Idempotent write operations
- Synchronous + asynchronous communication patterns
- Infrastructure as code (Docker, Kubernetes, Terraform)

See `docs/architecture.md` for detailed flow diagrams and service interaction explanations.

---

## Repository Overview

This repository contains:

- **services/** — Backend microservices (API Gateway, Payments Core, Ledger, Webhook)
- **frontend/** — Admin dashboard for payment lifecycle visibility and operational tooling
- **libs/** — Shared libraries, API contracts (proto/OpenAPI), auth, idempotency, observability
- **infra/** — Docker, Kubernetes, and Terraform for deployment
- **docs/** — Architecture, payment lifecycle, ledger invariants, production-readiness
- **scripts/** — Formatting, linting, and test runner helpers
- **.github/** — CI workflows (GitHub Actions)

---

## Core Engineering Concepts Modeled

- Strong domain modeling with explicit payment states
- Safe state transitions via controlled lifecycle management
- Double-entry bookkeeping and accounting invariants
- Immutable financial records
- Idempotent API design to prevent duplicate charges
- Service boundaries and multi-language architecture
- Production deployment and CI workflows

---

## Prerequisites

- Go 1.21+
- Java 17+ (for payments-core)
- Scala 2.13+ / sbt (for ledger-service)
- Ruby 3.x (for webhook-service)
- Node.js 18+ (for frontend)
- Docker and Docker Compose (for local orchestration)

---

## Quick Start

```bash
# Install dependencies and run all services locally
make deps
make up

# Run tests
make test

# Lint and format
make lint
make fmt
