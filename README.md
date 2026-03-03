# payments-platform

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

```
payments-platform/
├── services/
│   ├── api-gateway/        → Go
│   ├── payments-core/      → Java
│   ├── ledger-service/     → Scala
│   └── webhook-service/    → Ruby
├── frontend/               → Admin dashboard (Next.js)
├── libs/                   → Shared contracts, auth, idempotency, observability
├── infra/                  → Docker, Kubernetes, Terraform
├── docs/                   → Architecture, payment lifecycle, ledger invariants
├── scripts/                → Lint, format, test runners
└── .github/                → CI workflows
```

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

| Tool | Version |
|------|---------|
| Java | 17+ |
| Gradle | 8+ (or use the included wrapper) |
| Go | 1.21+ |
| Scala / sbt | 2.13+ |
| Ruby | 3.x |
| Node.js | 18+ |
| Docker + Docker Compose | Latest |

---

## What Is Built So Far

### Phase 1 — Payments Core: Domain Layer ✅

Pure Java domain model for the payment lifecycle. No database, no HTTP, no frameworks.

| Class | Location |
|-------|----------|
| `Money` | `services/payments-core/src/main/java/.../domain/Money.java` |
| `PaymentStatus` | `services/payments-core/src/main/java/.../domain/PaymentStatus.java` |
| `PaymentIntent` | `services/payments-core/src/main/java/.../domain/PaymentIntent.java` |
| `IdempotencyKey` | `services/payments-core/src/main/java/.../domain/IdempotencyKey.java` |
| `InvalidStateTransitionException` | `.../domain/exceptions/` |
| `InvalidMoneyException` | `.../domain/exceptions/` |
| `DuplicateIdempotencyKeyException` | `.../domain/exceptions/` |

Full details in [`docs/FEATURES.md`](docs/FEATURES.md).

---

## Running the Tests (Phase 1)

All domain logic is covered by unit tests. No infrastructure (database, broker, Docker) is needed to run them.

### Run payments-core domain tests

```bash
cd services/payments-core

# Using the Gradle wrapper (recommended)
./gradlew test

# Using system Gradle
gradle test
```

On Windows (Git Bash / WSL):
```bash
./gradlew test
```

On Windows (Command Prompt or PowerShell):
```cmd
gradlew.bat test
```

### View test results

After the run, open the HTML report:
```
services/payments-core/build/reports/tests/test/index.html
```

### Run a specific test class

```bash
./gradlew test --tests "com.paymentsplatform.paymentscore.domain.MoneyTest"
./gradlew test --tests "com.paymentsplatform.paymentscore.domain.PaymentStatusTest"
./gradlew test --tests "com.paymentsplatform.paymentscore.domain.PaymentIntentTest"
```

### Expected output

```
> Task :test

com.paymentsplatform.paymentscore.domain.MoneyTest > createsMoneyWithValidBigDecimalAndCurrency PASSED
com.paymentsplatform.paymentscore.domain.MoneyTest > addsTwoMoneyValuesOfSameCurrency PASSED
...
com.paymentsplatform.paymentscore.domain.PaymentStatusTest > createdCanTransitionToProcessing PASSED
...
com.paymentsplatform.paymentscore.domain.PaymentIntentTest > happyPath_created_processing_succeeded PASSED
...

BUILD SUCCESSFUL
```

---

## Running All Services Locally (Docker Compose)

> Note: Most services are not yet implemented. This starts the infrastructure only.

```bash
# Install dependencies
make deps

# Start all services + infrastructure
make up

# Stop everything
make down
```

---

## Development Roadmap

See [`PLAN.md`](PLAN.md) for the full 14-phase development roadmap.

| Phase | Focus | Status |
|-------|-------|--------|
| 1 | Payments Core — Domain | ✅ Complete |
| 2 | Payments Core — Persistence | Pending |
| 3 | Payments Core — HTTP | Pending |
| 4 | Ledger Service — Domain | Pending |
| 5 | Ledger Service — Persistence + HTTP | Pending |
| 6 | Event Integration (RabbitMQ) | Pending |
| 7 | Webhook Service | Pending |
| 8 | API Gateway | Pending |
| 9 | Shared Libraries | Pending |
| 10 | Observability | Pending |
| 11 | Database Migrations | Pending |
| 12 | Frontend Dashboard | Pending |
| 13 | CI/CD | Pending |
| 14 | E2E Testing + Production Readiness | Pending |

---

## Project Documentation

| File | Purpose |
|------|---------|
| [`PLAN.md`](PLAN.md) | Phase-by-phase development roadmap |
| [`docs/FEATURES.md`](docs/FEATURES.md) | Implemented features by service and phase |
| [`docs/architecture.md`](docs/architecture.md) | System architecture and service flows |
| [`docs/payment-lifecycle.md`](docs/payment-lifecycle.md) | Payment state machine documentation |
| [`docs/ledger-invariants.md`](docs/ledger-invariants.md) | Accounting invariants for the ledger service |
| [`CLAUDE.md`](CLAUDE.md) | AI assistant knowledgebase for this project |
