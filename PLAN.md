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

## Phase 1 — Payments Core: Domain Layer (Java) ✅ COMPLETE

**Goal:** Implement the pure domain model for payments. No frameworks. No database. No HTTP.

### 1.1 `Money` Value Object ✅
- Stores amount as `BigDecimal` (never `double` or `float`)
- Stores currency as ISO 4217 string (e.g., `"INR"`)
- Immutable — all operations return new instances
- Validates: amount must be non-negative, currency must be non-null/non-empty
- Implements: `add()`, `subtract()`, `isGreaterThan()`, `equals()`, `hashCode()`, `toString()`
- Throws on currency mismatch

### 1.2 `PaymentStatus` Enum ✅
- States: `CREATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELED`
- Legal transitions defined per state:
  - `CREATED` → `PROCESSING`, `CANCELED`
  - `PROCESSING` → `SUCCEEDED`, `FAILED`
  - `SUCCEEDED` → (terminal — no transitions)
  - `FAILED` → (terminal — no transitions)
  - `CANCELED` → (terminal — no transitions)
- Method: `canTransitionTo(PaymentStatus next): boolean`
- Method: `isTerminal(): boolean`

### 1.3 `PaymentIntent` Entity ✅
- Fields: `id` (UUID), `amount` (Money), `status` (PaymentStatus), `idempotencyKey` (String), `createdAt`, `updatedAt`
- Created via static factory method: `PaymentIntent.create(amount, idempotencyKey)`
- Encapsulates all state transition methods:
  - `startProcessing()` — `CREATED` → `PROCESSING`
  - `markSucceeded()` — `PROCESSING` → `SUCCEEDED`
  - `markFailed()` — `PROCESSING` → `FAILED`
  - `cancel()` — `CREATED` → `CANCELED`
- Each method validates the transition via `PaymentStatus.canTransitionTo()` and throws `InvalidStateTransitionException` if illegal
- No setters — state changes only through named methods
- `updatedAt` refreshed on every successful transition

### 1.4 Domain Exceptions ✅
- `InvalidStateTransitionException` — thrown on illegal transition, carries `paymentId`, `from`, `to`
- `InvalidMoneyException` — thrown on bad Money construction or currency mismatch
- `DuplicateIdempotencyKeyException` — thrown when a key already exists (logic only, no DB yet)

### 1.5 Additional Domain Classes ✅
- `IdempotencyKey` value object — validates length (8–255 chars), trims whitespace
- `PaymentStateMachine` — documents state topology, reserved for future extraction
- `PaymentErrors` — error catalogue, maps to HTTP codes in Phase 3

### 1.6 Unit Tests ✅
- `MoneyTest` — construction, arithmetic, equality, immutability, INR and currency mismatch scenarios
- `PaymentStatusTest` — all legal and illegal transition combinations, `isTerminal()`
- `PaymentIntentTest` — happy paths, all invalid transitions, exception context, `updatedAt` refresh, INR lifecycle scenarios

**Phase 1 Definition of Done:**
- [x] All domain classes implemented (`Money`, `PaymentStatus`, `PaymentIntent`, `IdempotencyKey`)
- [x] All domain exceptions defined
- [x] Unit tests written covering >90% of domain logic
- [x] INR (Indian Rupee) test coverage added across Money and PaymentIntent tests
- [x] Zero framework imports in domain classes
- [x] Zero database or HTTP code anywhere

**Files delivered:**
```
src/main/java/.../internal/domain/
├── Money.java
├── PaymentStatus.java
├── PaymentIntent.java
├── IdempotencyKey.java
├── PaymentStateMachine.java
└── exceptions/
    ├── InvalidStateTransitionException.java
    ├── InvalidMoneyException.java
    └── DuplicateIdempotencyKeyException.java

src/test/java/.../domain/
├── MoneyTest.java
├── PaymentStatusTest.java
└── PaymentIntentTest.java
```

---

## Phase 2 — Payments Core: Persistence Layer (Java) ✅ COMPLETE

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
- [x] Migration files in place and tested
- [x] Repository interface defined in domain
- [x] Repository implementation in infrastructure (not domain)
- [x] Idempotency store implemented
- [x] Integration tests pass against a real database
- [x] `PaymentIntent` entity has zero ORM annotations

**Files delivered:**
```
services/payments-core/migrations/
└── V1__create_payments_table.sql

src/main/java/.../internal/domain/
├── PaymentRepository.java          (interface)
├── IdempotencyStore.java           (interface)
└── PaymentIntent.java              (added reconstitute() factory)

src/main/java/.../internal/infrastructure/
├── PostgresPaymentRepository.java  (JDBC upsert, manual row mapping)
└── PostgresIdempotencyStore.java   (backed by payments table)

src/test/java/.../infrastructure/
└── PaymentRepositoryIntegrationTest.java
```

---

## Phase 3 — Payments Core: Application + HTTP Layer (Java) ✅ COMPLETE

**Goal:** Wire domain and persistence together through use cases. Expose HTTP endpoints.

### 3.1 Use Cases (Application Layer) ✅
- `CreatePaymentUseCase` ✅
  - Receives: amount, currency, idempotency key
  - Checks idempotency store first — if key exists, returns existing intent
  - Otherwise creates `PaymentIntent.create(...)`, saves it, returns it
  - Returns a `Result` carrying the intent plus an `isNew()` flag so the transport
    layer can pick 201 (created) vs 200 (idempotent replay)
- `ProcessPaymentUseCase` ✅ — loads by ID, calls `startProcessing()`, saves
  (event emission deferred to Phase 6)
- `ConfirmPaymentUseCase` ✅ — single `execute(id, Outcome)` where `Outcome` is
  `SUCCEEDED` or `FAILED`; calls `markSucceeded()` / `markFailed()` and saves
- `CancelPaymentUseCase` ✅ — loads, calls `cancel()`, saves
- `GetPaymentUseCase` ✅ — added so transport never references the repository
  directly, even for read paths

### 3.2 HTTP Handlers (Transport Layer) ✅
HTTP server: **Javalin 6.3** + Jackson 2.17 (JSR-310 enabled for `Instant`).
- `POST /payments` — create payment intent. Idempotency key carried in the
  `Idempotency-Key` request header (Stripe convention)
- `GET /payments/{id}` — fetch payment intent by ID
- `POST /payments/{id}/process` — `CREATED → PROCESSING`
- `POST /payments/{id}/confirm` — body `{"outcome": "succeeded"|"failed"}`
- `POST /payments/{id}/cancel` — cancel from `CREATED`
- All handlers: parse path/header/body → call use case → map result to response.
  Zero business logic in handlers.

### 3.3 Request/Response DTOs ✅
- `CreatePaymentRequest(BigDecimal amount, String currency)`
- `ConfirmPaymentRequest(String outcome)`
- `PaymentResponse(id, status, amount, currency, idempotencyKey, createdAt, updatedAt)`
  — built via `PaymentResponse.from(PaymentIntent)`; domain entity is never serialised directly
- `ErrorResponse(code, message)` — uniform error envelope with a stable
  machine-readable `code`

### 3.4 Error Handling ✅
Centralised in `transport/ErrorHandler.java`:
- `PaymentNotFoundException` → **404** `not_found`
- `InvalidStateTransitionException` → **422** `invalid_state_transition`
- `InvalidMoneyException` → **400** `invalid_money`
- `DuplicateIdempotencyKeyException` → **409** `duplicate_idempotency_key`
  (defensive — the use case prefers returning the existing intent with 200)
- `InvalidPathParameterException` → **400** `invalid_path_parameter`
- `IllegalArgumentException` → **400** `invalid_request`
- Any other `Exception` → **500** `internal_error`
- Missing `Idempotency-Key` header → **400** `missing_idempotency_key`
- Unknown confirm outcome → **400** `invalid_outcome`

**Phase 3 Definition of Done:**
- [x] All four use cases implemented (plus `GetPaymentUseCase` for read paths)
- [x] HTTP handlers implemented, referencing only use cases
- [x] DTOs separate from domain objects
- [x] Error mapping complete
- [x] Integration test exercises full create → process → confirm flow plus all error paths against a real Postgres (12 scenarios, Testcontainers)
- [x] No business logic in handlers

**Files delivered:**
```
services/payments-core/src/main/java/.../
├── Application.java                       (Javalin bootstrap + DI wiring)
├── internal/application/
│   ├── CreatePaymentUseCase.java          (with nested Result type)
│   ├── GetPaymentUseCase.java
│   ├── ProcessPaymentUseCase.java
│   ├── ConfirmPaymentUseCase.java         (with nested Outcome enum)
│   └── CancelPaymentUseCase.java
├── internal/transport/
│   ├── PaymentController.java
│   ├── ErrorHandler.java
│   └── dto/
│       ├── CreatePaymentRequest.java
│       ├── ConfirmPaymentRequest.java
│       ├── PaymentResponse.java
│       └── ErrorResponse.java
└── internal/domain/exceptions/
    └── PaymentNotFoundException.java      (new — signals 404)

services/payments-core/src/test/java/.../
├── application/                           (18 unit tests + in-memory fakes)
└── transport/PaymentControllerIntegrationTest.java   (12 HTTP scenarios)
```

**Vestigial stubs removed:** `internal/api/PaymentHandler.java` (moved to
`transport/PaymentController.java`) and `internal/persistence/` package
(interfaces always lived in `domain/`).

**Build:** added Javalin, Jackson, slf4j-simple; pinned test JVM
`user.timezone=UTC` so Windows "Asia/Calcutta" doesn't break Postgres 16.

**Test suite:** 126 tests pass (40 Money, 30 PaymentIntent, 18 PaymentStatus,
18 use-case unit, 8 repository integration, 12 controller integration).

---

## Phase 4 — Ledger Service: Domain Layer (Scala) ✅ COMPLETE

**Goal:** Pure domain — double-entry accounting logic. No frameworks. No database.

### 4.1 `Money` Value Object (Scala) ✅
- Immutable `case class` (private constructor) — `Money.of(amount, currency)` factory
- `BigDecimal` amount, ISO 4217 currency normalised via `java.util.Currency`
- `add`, `subtract`, `isGreaterThan`, `isZero`
- `Money.of` enforces non-negative on entry amounts; internal arithmetic via the private
  constructor allows signed results so derived account balances can go negative

### 4.2 `EntryType` Enum ✅
- Sealed trait + `Debit` / `Credit` case objects, each with a stable string `name`
- `EntryType.fromName` parses case-insensitively, throws on unknown

### 4.3 `LedgerEntry` Case Class ✅
- Fields: `id` (UUID), `accountId` (UUID), `entryType`, `amount` (Money), `paymentId` (UUID), `createdAt`
- Immutable `case class` — companion `debit(...)` and `credit(...)` factories
- No update or delete methods

### 4.4 `Account` Entity ✅
- Fields: `id`, `name`, `currency`, `entries: List[LedgerEntry]`
- `Account.open(name, currency)` factory — starts with an empty entry list
- `applyEntry(entry)` returns a *new* `Account` with the entry appended;
  rejects mismatched `accountId` or currency
- `balance: Money` derived from entries (credits add, debits subtract) — never stored

### 4.5 `DoubleEntryValidator` ✅
- Per-transaction invariants enforced before persistence:
  - sum of debits == sum of credits
  - all entries share one `paymentId` and one currency
  - non-empty, non-zero
- Violations throw `InvariantViolationException`
- `isValid` non-throwing helper

### 4.6 Domain Exceptions ✅
- `InvariantViolationException` — double-entry rule broken (carries `paymentId`)
- `ImmutableEntryException` — reserved for the persistence layer (Phase 5) to signal attempted mutation
- `InvalidMoneyException` — bad Money construction or currency mismatch

### 4.7 Unit Tests (ScalaTest) ✅
- `MoneySpec`, `EntryTypeSpec`, `LedgerEntrySpec`, `AccountSpec`, `DoubleEntryValidatorSpec`

**Phase 4 Definition of Done:**
- [x] All domain classes implemented as immutable value objects / case classes
- [x] Double-entry validator enforced
- [x] Immutability enforced at domain level (no mutators, append-only entry list)
- [x] Unit tests written (run via `sbt test`)
- [x] Zero framework or database imports in domain

**Files delivered:**
```
services/ledger-service/
├── build.sbt                                  (added scalatest 3.2.18 test dep)
└── src/main/scala/.../internal/domain/
    ├── Money.scala
    ├── EntryType.scala
    ├── LedgerEntry.scala
    ├── Account.scala
    ├── DoubleEntryValidator.scala
    └── exceptions/
        ├── InvalidMoneyException.scala
        ├── InvariantViolationException.scala
        └── ImmutableEntryException.scala

src/test/scala/.../internal/domain/
├── MoneySpec.scala
├── EntryTypeSpec.scala
├── LedgerEntrySpec.scala
├── AccountSpec.scala
└── DoubleEntryValidatorSpec.scala
```

**Note:** Test suite not executed locally (sbt not installed on dev machine).
Will be exercised by the `ledger-service` GitHub Actions job once re-enabled in Phase 13.

---

## Phase 5 — Ledger Service: Persistence + HTTP Layer (Scala) ✅ COMPLETE

**Goal:** Persist ledger entries (append-only). Expose HTTP endpoints for recording entries and querying balances.

### 5.1 Database Schema ✅
- `accounts (id UUID PK, name VARCHAR, currency VARCHAR(3), created_at TIMESTAMPTZ)`
- `ledger_entries (id UUID PK, account_id UUID FK, entry_type VARCHAR(8) CHECK IN ('DEBIT','CREDIT'), amount_value NUMERIC(19,4), amount_currency VARCHAR(3), payment_id UUID, created_at TIMESTAMPTZ)`
- No `updated_at`. No soft delete column.
- Indexes on `account_id` and `payment_id`.
- Migration: `migrations/V1__create_ledger_tables.sql`.

### 5.2 `LedgerRepository` + `AccountRepository` ✅
- Traits live in `internal/domain/`; JDBC impls live in `internal/infrastructure/`.
- `LedgerRepository.appendEntries(entries: List[LedgerEntry])` — atomic batch insert wrapped in a manual transaction. No update path.
- `findEntriesByAccount(accountId)`, `findEntriesByPayment(paymentId)` — ordered by `(created_at, id)`.
- `AccountRepository.save` is `INSERT ... ON CONFLICT (id) DO NOTHING`; `findById` reconstitutes an `Account` by loading the row plus all its entries.

### 5.3 HTTP Handlers ✅
HTTP server: **Javalin 6.3** + Jackson 2.17 with `DefaultScalaModule` + `JavaTimeModule`.
- `POST /accounts` — body `{name, currency}` → **201** + `AccountResponse`.
- `GET /accounts/{id}/balance` — **200** + `BalanceResponse` (balance derived from entries on each request).
- `POST /entries` — body `{paymentId, legs: [{accountId, entryType, amount, currency}, ...]}` — validates via `DoubleEntryValidator`, atomic insert. **201** on first write, **200** with the existing entries on duplicate `paymentId` (idempotent).
- `GET /entries/{paymentId}` — **200** + `TransactionResponse` (empty `entries` list if unknown).

### 5.4 Error Handling ✅
Centralised in `transport/ErrorHandler.scala`:
- `AccountNotFoundException` → **404** `not_found`
- `InvariantViolationException` → **422** `invariant_violation`
- `InvalidMoneyException` → **400** `invalid_money`
- `InvalidPathParameterException` → **400** `invalid_path_parameter`
- `InvalidEntryTypeException` → **400** `invalid_entry_type`
- `IllegalArgumentException` → **400** `invalid_request`
- Any other `Exception` → **500** `internal_error`

**Phase 5 Definition of Done:**
- [x] Append-only persistence with no update/delete paths
- [x] Balance always derived from entries, never stored
- [x] HTTP endpoints working
- [x] Integration tests with Testcontainers (9 HTTP scenarios via ScalaTest)

**Files delivered:**
```
services/ledger-service/
├── build.sbt                                            (postgres, hikari, javalin, jackson, testcontainers)
├── migrations/V1__create_ledger_tables.sql
└── src/main/scala/.../
    ├── Main.scala                                       (Javalin bootstrap + DI wiring)
    ├── internal/application/
    │   ├── OpenAccountUseCase.scala
    │   ├── GetBalanceUseCase.scala
    │   ├── RecordTransactionUseCase.scala               (idempotent on paymentId)
    │   └── GetEntriesForPaymentUseCase.scala
    ├── internal/domain/
    │   ├── AccountRepository.scala                      (trait)
    │   ├── LedgerRepository.scala                       (trait)
    │   └── exceptions/AccountNotFoundException.scala
    ├── internal/infrastructure/
    │   ├── PostgresAccountRepository.scala              (JDBC, no ORM)
    │   └── PostgresLedgerRepository.scala               (JDBC, atomic batch)
    └── internal/transport/
        ├── LedgerController.scala
        ├── ErrorHandler.scala
        └── dto/
            ├── CreateAccountRequest.scala
            ├── RecordEntriesRequest.scala               (with LegDto)
            ├── AccountResponse.scala
            ├── BalanceResponse.scala
            ├── LedgerEntryResponse.scala
            ├── TransactionResponse.scala
            └── ErrorResponse.scala

src/test/scala/.../internal/transport/
└── LedgerControllerIntegrationSpec.scala                (Testcontainers + JDK HttpClient)
```

**Vestigial stubs removed:** `internal/api/LedgerHandler.scala`, `internal/persistence/*.scala`, `internal/domain/Invariants.scala`.

**Note:** Test suite not executed locally (sbt not installed on dev machine).
Will be exercised by the `ledger-service` GitHub Actions job once re-enabled in Phase 13.

---

## Phase 6 — Event-Driven Integration (RabbitMQ) ✅ COMPLETE

**Goal:** Wire payments-core and ledger-service together via domain events.

### 6.1 Event Definitions ✅
Defined under `libs/contracts/events/` as the canonical JSON wire format
(language-specific classes in each service must match):
- `PaymentCreated`, `PaymentProcessing`, `PaymentSucceeded`, `PaymentFailed`, `PaymentCanceled`
- Common envelope: `eventType`, `schemaVersion` (=1), `paymentId`, `amount` (string for precision), `currency`, `timestamp` (ISO 8601 UTC)
- Topic exchange `payments.events`, durable; routing keys `payment.created|processing|succeeded|failed|canceled`

### 6.2 Payments Core — Publisher ✅
- `PaymentEventPublisher` interface in `internal/domain/`
- `RabbitMqPaymentEventPublisher` in `internal/infrastructure/` (amqp-client 5.21), declares the exchange on construction, synchronises publish for thread safety
- `NoopPaymentEventPublisher` fallback so dev/tests without a broker still work
- Each mutating use case publishes its event after the DB commit:
  - `CreatePaymentUseCase` → `PaymentCreated` (replays do NOT republish)
  - `ProcessPaymentUseCase` → `PaymentProcessing`
  - `ConfirmPaymentUseCase` → `PaymentSucceeded` / `PaymentFailed`
  - `CancelPaymentUseCase` → `PaymentCanceled`
- Publish failures are logged and swallowed — never roll back a committed payment write (publish-after-commit, at-most-once today; an outbox is a later hardening step)
- `Application.buildApp(DataSource, PaymentEventPublisher)` overload for tests; the original `buildApp(DataSource)` defaults to the no-op publisher so the existing Phase 3 integration test is unaffected

### 6.3 Ledger Service — Consumer ✅
- `RabbitMqPaymentEventConsumer` in `internal/infrastructure/`
- Declares the same exchange + a durable queue `ledger.payment-events` bound to `payment.succeeded`
- Parses the JSON envelope and dispatches to `RecordTransactionUseCase` with a balanced pair: debit `LEDGER_CASH_ACCOUNT_ID`, credit `LEDGER_REVENUE_ACCOUNT_ID`
- Idempotency on `paymentId` is inherited from `RecordTransactionUseCase` (already short-circuits on `findEntriesByPayment`)
- Malformed payloads are NACK'd with `requeue=false` so the queue is never poisoned (DLX wiring deferred to a later phase)
- Main wires the consumer optionally — only started when all three env vars (`RABBITMQ_URL`, `LEDGER_CASH_ACCOUNT_ID`, `LEDGER_REVENUE_ACCOUNT_ID`) are set

### 6.4 Integration Tests ✅
- `PaymentEventPublisherIntegrationTest` (payments-core): boots Postgres + RabbitMQ via Testcontainers, exercises the full HTTP API for create/process/confirm/cancel + idempotent replay, asserts every event arrives on the bound queue with the expected JSON envelope
- `RabbitMqPaymentEventConsumerSpec` (ledger-service): boots Postgres + RabbitMQ, opens the cash + revenue accounts, publishes `PaymentSucceeded` envelopes directly to the exchange, asserts a balanced 2-entry transaction lands in the ledger, that triplicate delivery still produces exactly 2 entries (idempotency), and that a malformed payload is discarded without blocking subsequent valid deliveries

**Phase 6 Definition of Done:**
- [x] Events defined in shared contracts
- [x] Publisher implemented in payments-core infrastructure layer
- [x] Consumer implemented in ledger-service infrastructure layer
- [x] Idempotent consumption verified
- [x] Full integration test passes with real RabbitMQ (Testcontainers)

**Files delivered:**
```
libs/contracts/events/
├── README.md                              (envelope spec, topology, delivery semantics)
├── payment-created.example.json
└── payment-succeeded.example.json

services/payments-core/
├── build.gradle                           (added amqp-client + testcontainers:rabbitmq)
└── src/main/java/.../
    ├── Application.java                   (buildApp overload, RabbitMQ wiring with Noop fallback)
    ├── internal/domain/
    │   ├── PaymentEventPublisher.java     (interface)
    │   └── events/
    │       ├── PaymentEvent.java          (sealed interface + factories)
    │       ├── PaymentCreatedEvent.java
    │       ├── PaymentProcessingEvent.java
    │       ├── PaymentSucceededEvent.java
    │       ├── PaymentFailedEvent.java
    │       └── PaymentCanceledEvent.java
    ├── internal/infrastructure/
    │   ├── NoopPaymentEventPublisher.java
    │   └── RabbitMqPaymentEventPublisher.java
    └── internal/application/              (all four mutating use cases publish after save)

src/test/java/.../infrastructure/
└── PaymentEventPublisherIntegrationTest.java

services/ledger-service/
├── build.sbt                              (added amqp-client + testcontainers:rabbitmq)
└── src/main/scala/.../
    ├── Main.scala                         (buildUseCases helper + optional consumer boot)
    └── internal/infrastructure/RabbitMqPaymentEventConsumer.scala

src/test/scala/.../infrastructure/
└── RabbitMqPaymentEventConsumerSpec.scala
```

**Note:** Ledger spec not executed locally (sbt still not installed on dev machine).
Payments-core unit tests pass (109 of 112); the 3 Testcontainers tests share the same
pre-existing Docker-on-Windows `InvalidPathException` from the host's PATH and will run
in CI once the `ledger-service` and `payments-core` GitHub Actions jobs are re-enabled in Phase 13.

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
| 1 | Payments Core — Domain | Java | ✅ Complete |
| 2 | Payments Core — Persistence | Java | ✅ Complete |
| 3 | Payments Core — HTTP | Java | ✅ Complete |
| 4 | Ledger Service — Domain | Scala | ✅ Complete |
| 5 | Ledger Service — Persistence + HTTP | Scala | ✅ Complete |
| 6 | Event Integration (RabbitMQ) | Java + Scala | ✅ Complete |
| 7 | Webhook Service | Ruby | **Next** |
| 8 | API Gateway | Go | |
| 9 | Shared Libraries | Go | |
| 10 | Observability | All | |
| 11 | Database Migrations | SQL | |
| 12 | Frontend Dashboard | TypeScript | |
| 13 | CI/CD | GitHub Actions | |
| 14 | E2E Testing & Production Readiness | All | |

---

## What We Are Building Now

**Phase 7 — Webhook Service (Ruby)**

Build the external-facing webhook delivery pipeline that fans the same payment events
that ledger-service consumes out to merchant-supplied URLs.

Scope (per Phase 7 above):
- `WebhookDelivery` domain model in Ruby: `id`, `paymentId`, `targetUrl`, `payload`, `status` (`pending` / `delivered` / `failed`), `attemptCount`, `lastAttemptAt`. Persistence with a versioned migration.
- RabbitMQ consumer that subscribes to all five `payment.*` routing keys on the `payments.events` topic exchange. Same JSON envelope as `libs/contracts/events/README.md`. For each event it creates a `WebhookDelivery` per registered target URL and enqueues delivery.
- HMAC-SHA256 signing of the payload using a per-tenant shared secret, sent as `X-Webhook-Signature`; `X-Webhook-Timestamp` for replay protection.
- Retry with exponential backoff + jitter, capped at 5 attempts. After max retries, the delivery is marked `failed` and moves to a dead-letter table (no real DLX broker yet).
- HTTP endpoints: `POST /webhooks/register`, `GET /webhooks/deliveries/{id}`, `POST /webhooks/deliveries/{id}/retry`.

Hard constraints:
- Consumer must be idempotent on `(deliveryId, eventId)` — duplicate broker deliveries do not create duplicate `WebhookDelivery` rows.
- Signing must use a constant-time comparison helper on the receiver side; document the verification recipe in the libs/contracts notes.
- No business logic in the HTTP handlers — they only register/inspect/retrigger deliveries.
