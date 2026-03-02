# Development Plan — payments-platform

This is the canonical development roadmap for the project. Follow phases in order. Do not skip ahead.

Each phase has a clear goal, a definition of done, and explicit boundaries on what is and is not in scope.

---

## Guiding Rules

- Build domain logic before infrastructure. Never mix them.
- Each phase must be complete and tested before the next begins.
- No database code until Phase 2. No HTTP code until Phase 3.
- All money values use the `Money` value object — never primitives or floats.
- Ledger entries are always immutable. State transitions are always explicit.
- Write tests alongside the code, not after.

---

## Phase 1 — Payments Core: Domain Layer (Java)

**Goal:** Implement the pure domain model for payments. No frameworks. No database. No HTTP.

### 1.1 `Money` Value Object
- Stores amount as `BigDecimal` (never `double` or `float`)
- Stores currency as ISO 4217 string (e.g., `"USD"`)
- Immutable — all operations return new instances
- Validates: amount must be non-negative, currency must be non-null/non-empty
- Implements: `add()`, `subtract()`, `isGreaterThan()`, `equals()`, `hashCode()`, `toString()`
- Throws on currency mismatch

### 1.2 `PaymentStatus` Enum
- States: `CREATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELED`
- Define which transitions are legal from each state:
  - `CREATED` → `PROCESSING`, `CANCELED`
  - `PROCESSING` → `SUCCEEDED`, `FAILED`
  - `SUCCEEDED` → (terminal — no transitions)
  - `FAILED` → (terminal — no transitions)
  - `CANCELED` → (terminal — no transitions)
- Method: `canTransitionTo(PaymentStatus next): boolean`

### 1.3 `PaymentIntent` Entity
- Fields: `id` (UUID), `amount` (Money), `status` (PaymentStatus), `idempotencyKey` (String), `createdAt`, `updatedAt`
- Created via static factory method: `PaymentIntent.create(amount, idempotencyKey)`
- Encapsulates all state transition methods:
  - `startProcessing()` — `CREATED` → `PROCESSING`
  - `markSucceeded()` — `PROCESSING` → `SUCCEEDED`
  - `markFailed()` — `PROCESSING` → `FAILED`
  - `cancel()` — `CREATED` or `PROCESSING` → `CANCELED`
- Each method validates the transition via `PaymentStatus.canTransitionTo()` and throws `InvalidStateTransitionException` if illegal
- No setters — state changes only through named methods

### 1.4 Domain Exceptions
- `InvalidStateTransitionException` — thrown on illegal transition
- `InvalidMoneyException` — thrown on bad Money construction
- `DuplicateIdempotencyKeyException` — thrown when a key already exists (logic only, no DB yet)

### 1.5 Unit Tests
- `MoneyTest` — construction, arithmetic, equality, currency mismatch
- `PaymentStatusTest` — all legal and illegal transition combinations
- `PaymentIntentTest` — state machine paths: happy path, all failure branches, terminal state enforcement

**Phase 1 Definition of Done:**
- [ ] All three domain classes implemented
- [ ] All domain exceptions defined
- [ ] Unit tests pass with >90% coverage of domain logic
- [ ] Zero framework imports in domain classes
- [ ] Zero database or HTTP code anywhere

---

## Phase 2 — Payments Core: Persistence Layer (Java)

**Goal:** Add the repository and idempotency store. Domain must not change.

### 2.1 Database Schema
- Migration: `payments` table
  - `id UUID PRIMARY KEY`
  - `idempotency_key VARCHAR UNIQUE NOT NULL`
  - `amount_value NUMERIC(19,4) NOT NULL`
  - `amount_currency VARCHAR(3) NOT NULL`
  - `status VARCHAR(20) NOT NULL`
  - `created_at TIMESTAMPTZ NOT NULL`
  - `updated_at TIMESTAMPTZ NOT NULL`
- Use Flyway or Liquibase for migrations

### 2.2 `PaymentRepository` Interface (in domain)
- `save(PaymentIntent): void`
- `findById(UUID): Optional<PaymentIntent>`
- `findByIdempotencyKey(String): Optional<PaymentIntent>`

### 2.3 `PaymentRepository` Implementation (in infrastructure)
- Implements the domain interface using JDBC or jOOQ (no ORM magic)
- Maps between database rows and domain objects manually
- No domain annotations on the `PaymentIntent` entity

### 2.4 `IdempotencyStore`
- `exists(String key): boolean`
- `save(String key, UUID paymentId): void`
- Backed by the same `payments` table or a separate `idempotency_keys` table

### 2.5 Integration Tests
- Spin up a real Postgres (use Testcontainers)
- Test round-trip: save → find by ID → find by idempotency key

**Phase 2 Definition of Done:**
- [ ] Migration files in place and tested
- [ ] Repository interface defined in domain
- [ ] Repository implementation in infrastructure (not domain)
- [ ] Idempotency store implemented
- [ ] Integration tests pass against a real database
- [ ] `PaymentIntent` entity has zero ORM annotations

---

## Phase 3 — Payments Core: Application + HTTP Layer (Java)

**Goal:** Wire domain and persistence together through use cases. Expose HTTP endpoints.

### 3.1 Use Cases (Application Layer)
- `CreatePaymentUseCase`
  - Receives: amount, currency, idempotency key
  - Checks idempotency store first — if key exists, return existing intent
  - Creates `PaymentIntent.create(...)`, saves it, returns it
- `ProcessPaymentUseCase`
  - Loads intent by ID, calls `startProcessing()`, saves, emits event
- `ConfirmPaymentUseCase`
  - Marks payment as succeeded or failed based on result
- `CancelPaymentUseCase`
  - Loads intent, calls `cancel()`, saves

### 3.2 HTTP Handlers (Transport Layer)
- `POST /payments` — create payment intent
- `GET /payments/{id}` — get payment intent by ID
- `POST /payments/{id}/process` — start processing
- `POST /payments/{id}/cancel` — cancel
- All handlers: validate input → call use case → map result to HTTP response
- Use standard HTTP status codes: 200, 201, 400, 404, 409 (conflict on duplicate key), 422

### 3.3 Request/Response DTOs
- Separate from domain objects — never expose domain internals directly
- Validation on input DTOs (required fields, format)

### 3.4 Error Handling
- Map domain exceptions to HTTP responses:
  - `InvalidStateTransitionException` → 422
  - `DuplicateIdempotencyKeyException` → 200 (return existing)
  - Not found → 404

**Phase 3 Definition of Done:**
- [ ] All four use cases implemented
- [ ] HTTP handlers implemented, referencing only use cases
- [ ] DTOs separate from domain objects
- [ ] Error mapping complete
- [ ] Postman or curl test: full create → process → succeed flow works end to end
- [ ] No business logic in handlers

---

## Phase 4 — Ledger Service: Domain Layer (Scala)

**Goal:** Pure domain — double-entry accounting logic. No frameworks. No database.

### 4.1 `Money` Value Object (Scala)
- Same semantics as Java version: `BigDecimal`, ISO currency, immutable
- Implemented as a `case class`

### 4.2 `EntryType` Enum
- `DEBIT`, `CREDIT`

### 4.3 `LedgerEntry` Value Object
- Fields: `id` (UUID), `accountId` (UUID), `type` (EntryType), `amount` (Money), `paymentId` (UUID), `createdAt`
- Immutable — `case class`
- No update or delete methods

### 4.4 `Account` Entity
- Fields: `id` (UUID), `name` (String), `entries` (List of LedgerEntry — empty initially)
- `balance(): Money` — derived by summing entries (credits add, debits subtract)
- `applyEntry(entry: LedgerEntry): Account` — returns new Account with entry appended (immutable)

### 4.5 Accounting Invariants (enforced before persistence)
- `DoubleEntryValidator`: for any transaction, sum of debits must equal sum of credits
- `ImmutabilityGuard`: no update or delete operations are ever permitted
- `BalanceInvariant`: balance is always derived, never stored

### 4.6 Domain Exceptions
- `InvariantViolationException` — thrown when double-entry rule is broken
- `ImmutableEntryException` — thrown if update/delete is attempted

### 4.7 Unit Tests (ScalaTest)
- `MoneySpec`, `LedgerEntrySpec`, `AccountSpec`
- `DoubleEntryValidatorSpec` — valid and invalid transaction sets

**Phase 4 Definition of Done:**
- [ ] All domain classes implemented as immutable value objects / case classes
- [ ] Double-entry validator enforced
- [ ] Immutability enforced at domain level
- [ ] Unit tests pass
- [ ] Zero framework or database imports in domain

---

## Phase 5 — Ledger Service: Persistence + HTTP Layer (Scala)

**Goal:** Persist ledger entries (append-only). Expose HTTP endpoints for recording entries and querying balances.

### 5.1 Database Schema
- `accounts` table: `id`, `name`, `created_at`
- `ledger_entries` table: `id`, `account_id`, `type`, `amount_value`, `amount_currency`, `payment_id`, `created_at`
- No `updated_at` — entries are immutable
- No soft deletes

### 5.2 `LedgerRepository`
- `appendEntry(entry: LedgerEntry): Unit` — insert only, never update
- `findEntriesByAccount(accountId: UUID): List[LedgerEntry]`
- `findEntriesByPayment(paymentId: UUID): List[LedgerEntry]`

### 5.3 HTTP Handlers
- `POST /accounts` — create account
- `GET /accounts/{id}/balance` — get derived balance
- `POST /entries` — record a ledger entry (validates double-entry pair in request body)
- `GET /entries/{paymentId}` — get entries for a payment

**Phase 5 Definition of Done:**
- [ ] Append-only persistence with no update/delete paths
- [ ] Balance always derived from entries, never stored
- [ ] HTTP endpoints working
- [ ] Integration tests with Testcontainers

---

## Phase 6 — Event-Driven Integration (RabbitMQ)

**Goal:** Wire payments-core and ledger-service together via domain events.

### 6.1 Event Definitions (in `libs/proto` or shared contracts)
- `PaymentCreatedEvent`
- `PaymentProcessingEvent`
- `PaymentSucceededEvent`
- `PaymentFailedEvent`
- `PaymentCanceledEvent`
- Each event includes: `paymentId`, `amount`, `currency`, `timestamp`

### 6.2 Payments Core — Publisher
- After each use case succeeds, publish the corresponding event to RabbitMQ
- Use a `PaymentEventPublisher` interface in domain, implemented in infrastructure
- Serialise events as JSON

### 6.3 Ledger Service — Consumer
- Subscribe to `PaymentSucceededEvent`
- On receipt: validate invariants, append debit/credit ledger entries for the payment
- Idempotent consumption — check if entries for `paymentId` already exist before writing

### 6.4 Integration Test
- Full flow: create payment → process → succeed → assert ledger entries created

**Phase 6 Definition of Done:**
- [ ] Events defined in shared contracts
- [ ] Publisher implemented in payments-core infrastructure layer
- [ ] Consumer implemented in ledger-service infrastructure layer
- [ ] Idempotent consumption verified
- [ ] Full integration test passes with real RabbitMQ (Testcontainers or Docker Compose)

---

## Phase 7 — Webhook Service (Ruby)

**Goal:** Implement event-driven webhook delivery with signing, retry, and tracking.

### 7.1 `WebhookDelivery` Domain Model
- Fields: `id`, `paymentId`, `targetUrl`, `payload`, `status` (`pending`, `delivered`, `failed`), `attemptCount`, `lastAttemptAt`

### 7.2 Event Consumer
- Subscribe to all payment events from RabbitMQ
- For each event, create a `WebhookDelivery` record and enqueue a delivery job

### 7.3 HMAC Signing
- Sign payload with HMAC-SHA256 using a shared secret
- Include signature in `X-Webhook-Signature` header
- Timestamp in `X-Webhook-Timestamp` header (for replay protection)

### 7.4 Delivery with Retry
- HTTP POST to `targetUrl` with signed payload
- On failure: retry with exponential backoff + jitter
- Max retries: 5
- After max retries: move to dead-letter, mark delivery as `failed`

### 7.5 HTTP Endpoints
- `POST /webhooks/register` — register a webhook endpoint URL
- `GET /webhooks/deliveries/{id}` — get delivery status
- `POST /webhooks/deliveries/{id}/retry` — manual retry trigger

**Phase 7 Definition of Done:**
- [ ] HMAC signing implemented and tested
- [ ] Retry logic with backoff and jitter implemented
- [ ] Dead-letter handling implemented
- [ ] Delivery status tracked in persistence
- [ ] Consumer wired to RabbitMQ
- [ ] HTTP endpoints working

---

## Phase 8 — API Gateway (Go)

**Goal:** Single entry point for all external traffic. No business logic.

### 8.1 Middleware (implement in order)
- `requestid.go` — generate `X-Request-ID` and inject into context
- `auth.go` — validate API key from `Authorization` header
- `ratelimit.go` — per-tenant rate limiting (token bucket, backed by Redis)
- `validation.go` — validate content type, required headers, body size

### 8.2 Routing
- `POST /v1/payments` → proxy to payments-core `POST /payments`
- `GET /v1/payments/{id}` → proxy to payments-core `GET /payments/{id}`
- `POST /v1/payments/{id}/cancel` → proxy to payments-core
- `GET /v1/accounts/{id}/balance` → proxy to ledger-service
- `POST /v1/webhooks/register` → proxy to webhook-service

### 8.3 HTTP Clients
- `PaymentsClient` — typed Go client for payments-core
- `LedgerClient` — typed Go client for ledger-service
- `WebhookClient` — typed Go client for webhook-service
- All clients: set `X-Request-ID` header on outbound requests, handle timeouts

### 8.4 Error Propagation
- Forward error status codes from downstream services
- Translate connection errors to `502 Bad Gateway`
- Translate timeouts to `504 Gateway Timeout`

**Phase 8 Definition of Done:**
- [ ] All middleware implemented and tested
- [ ] All routes proxy correctly to downstream services
- [ ] Typed clients implemented with timeout handling
- [ ] End-to-end request flow tested through the gateway
- [ ] No business logic in gateway — it only routes and enforces cross-cutting concerns

---

## Phase 9 — Shared Libraries

**Goal:** Extract reusable cross-cutting concerns into `libs/`.

### 9.1 `libs/auth/`
- Go auth validator: parse and validate API keys
- Auth specification: document key format and behavior

### 9.2 `libs/idempotency/`
- Go idempotency key parser and validator
- Document TTL, header name, reuse behavior

### 9.3 `libs/observability/`
- Go tracer: initialise OpenTelemetry tracer, propagate trace context
- Structured logging conventions (JSON, log levels, required fields)
- Metrics naming conventions (Prometheus-style)

### 9.4 `libs/proto/` and `libs/contracts/`
- Finalise Protobuf definitions for all inter-service messages
- Finalise OpenAPI specs for all services
- Generate client code from specs where applicable

**Phase 9 Definition of Done:**
- [ ] Auth and idempotency libraries usable by gateway and payments-core
- [ ] Observability tracer initialised in all Go services
- [ ] Proto and OpenAPI specs finalised and match implemented APIs

---

## Phase 10 — Observability

**Goal:** Make the system inspectable in production.

- Structured JSON logging in all services (correlation by `X-Request-ID`)
- Prometheus metrics: request count, latency histogram, error rate — per service
- OpenTelemetry distributed tracing: trace spans across gateway → payments-core → ledger-service
- Health check endpoints: `GET /health` returns `{"status":"ok"}` in all services
- Readiness endpoints: `GET /ready` — checks DB and broker connectivity

**Phase 10 Definition of Done:**
- [ ] Logs include `request_id`, `service`, `level`, `message`, `timestamp` in all services
- [ ] Prometheus metrics exposed at `/metrics` in all services
- [ ] Traces visible end-to-end across service boundaries
- [ ] Health and readiness endpoints working in all services

---

## Phase 11 — Database Migrations

**Goal:** Formalise schema management for all services.

- All schemas defined as versioned migration files (Flyway for Java/Scala, golang-migrate for Go)
- Migrations run automatically on service startup in dev, manually in production
- Migration filenames follow: `V{number}__{description}.sql`
- Test: fresh database + migrations applied = schema matches expectations

---

## Phase 12 — Frontend Admin Dashboard

**Goal:** Visibility into the payment lifecycle.

- Stack: Next.js + TypeScript
- Pages:
  - Payment list — table of payment intents with status
  - Payment detail — status history, ledger entries, webhook deliveries
  - Ledger view — account balances and entry history
- Data sourced from API Gateway
- No write operations from the frontend (read-only dashboard)
- Authentication: API key passed as Bearer token

---

## Phase 13 — CI/CD Pipelines (GitHub Actions)

**Goal:** Automated test and build pipeline on every PR.

### Per Service Workflow
- Trigger: push or PR to `main`
- Steps:
  1. Checkout code
  2. Set up language runtime (Go, Java, Scala, Ruby)
  3. Run linter
  4. Run unit tests
  5. Run integration tests (with Testcontainers or Docker Compose services)
  6. Build Docker image
  7. (On merge to main) Push image to registry

### Monorepo Strategy
- Use path filters — only run a service's workflow when its directory changes
- Shared lib changes trigger all service pipelines

---

## Phase 14 — End-to-End Testing & Production Readiness

**Goal:** Verify the full system works correctly as an integrated whole.

- E2E test suite: create payment → process → succeed → assert ledger entries → assert webhook delivered
- Load test: measure throughput and latency under concurrent requests
- Chaos test: kill a service mid-flow — verify idempotency allows safe retry
- Review all error paths end to end
- Document runbook: how to deploy, roll back, debug, and monitor

---

## Summary Table

| Phase | Focus | Language | Status |
|-------|-------|----------|--------|
| 1 | Payments Core — Domain | Java | **Start here** |
| 2 | Payments Core — Persistence | Java | |
| 3 | Payments Core — HTTP | Java | |
| 4 | Ledger Service — Domain | Scala | |
| 5 | Ledger Service — Persistence + HTTP | Scala | |
| 6 | Event Integration (RabbitMQ) | Java + Scala | |
| 7 | Webhook Service | Ruby | |
| 8 | API Gateway | Go | |
| 9 | Shared Libraries | Go | |
| 10 | Observability | All | |
| 11 | Database Migrations | SQL | |
| 12 | Frontend Dashboard | TypeScript | |
| 13 | CI/CD | GitHub Actions | |
| 14 | E2E Testing & Production Readiness | All | |

---

## What We Are Building Now

**Phase 1 — Payments Core Domain Layer**

Files to implement:
- `services/payments-core/internal/domain/Money.java`
- `services/payments-core/internal/domain/PaymentStatus.java`
- `services/payments-core/internal/domain/PaymentIntent.java`
- `services/payments-core/internal/domain/exceptions/InvalidStateTransitionException.java`
- `services/payments-core/internal/domain/exceptions/InvalidMoneyException.java`
- `services/payments-core/tests/domain/MoneyTest.java`
- `services/payments-core/tests/domain/PaymentStatusTest.java`
- `services/payments-core/tests/domain/PaymentIntentTest.java`
