# Features — payments-platform

This document tracks implemented features by service and phase.
Update this file each time a phase is completed.

---

## payments-core (Java)

### Phase 1 — Domain Layer ✅

**Status:** Complete

Pure domain model for payments. No frameworks, no database, no HTTP.

---

#### `Money` — Value Object
**File:** `internal/domain/Money.java`

Represents a monetary amount with an ISO 4217 currency code.

| Behaviour | Detail |
|-----------|--------|
| Immutable | All operations return new `Money` instances |
| Precision | Amount stored at 4 decimal places (`BigDecimal`, `HALF_UP`) |
| Validation | Amount must be non-negative; currency must be a 3-letter ISO 4217 code |
| Arithmetic | `add()`, `subtract()` — both return new instances |
| Comparison | `isGreaterThan()` |
| Guard | All operations across mismatched currencies throw `InvalidMoneyException` |
| Factory | `Money.of(BigDecimal, String)` and `Money.of(String, String)` |
| Supported currencies | Any ISO 4217 3-letter code — INR, USD, EUR, GBP, etc. |

```java
Money price = Money.of("1000.00", "INR");
Money tax   = Money.of("180.00",  "INR");  // 18% GST
Money total = price.add(tax);              // INR 1180.0000
```

---

#### `PaymentStatus` — Enum
**File:** `internal/domain/PaymentStatus.java`

Defines all legal states and transition rules for a payment.

| State | Terminal | Can transition to |
|-------|----------|-------------------|
| `CREATED` | No | `PROCESSING`, `CANCELED` |
| `PROCESSING` | No | `SUCCEEDED`, `FAILED` |
| `SUCCEEDED` | Yes | — |
| `FAILED` | Yes | — |
| `CANCELED` | Yes | — |

```
CREATED ──► PROCESSING ──► SUCCEEDED
   │                  └──► FAILED
   └──────────────────────► CANCELED
```

Key methods:
- `canTransitionTo(PaymentStatus)` — returns true if the transition is legal
- `isTerminal()` — returns true for SUCCEEDED, FAILED, CANCELED

---

#### `PaymentIntent` — Entity
**File:** `internal/domain/PaymentIntent.java`

Core domain entity. Owns the payment lifecycle and enforces all state transitions.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Auto-generated unique identifier |
| `amount` | `Money` | Monetary amount — immutable after creation |
| `idempotencyKey` | `String` | Client-supplied deduplication key |
| `status` | `PaymentStatus` | Current lifecycle state |
| `createdAt` | `Instant` | Set once at creation |
| `updatedAt` | `Instant` | Updated on every successful transition |

State transition methods:

| Method | Transition | Throws if illegal |
|--------|-----------|-------------------|
| `startProcessing()` | CREATED → PROCESSING | `InvalidStateTransitionException` |
| `markSucceeded()` | PROCESSING → SUCCEEDED | `InvalidStateTransitionException` |
| `markFailed()` | PROCESSING → FAILED | `InvalidStateTransitionException` |
| `cancel()` | CREATED → CANCELED | `InvalidStateTransitionException` |

```java
PaymentIntent intent = PaymentIntent.create(Money.of("4999", "INR"), "idem-key-001");
intent.startProcessing();
intent.markSucceeded();
// intent.getStatus() == PaymentStatus.SUCCEEDED
```

---

#### `IdempotencyKey` — Value Object
**File:** `internal/domain/IdempotencyKey.java`

Wraps a raw idempotency key string with validation.

- Length: 8–255 characters
- Whitespace trimmed on construction
- Equality by value, not reference

---

#### Domain Exceptions

| Exception | When thrown |
|-----------|-------------|
| `InvalidMoneyException` | Bad `Money` construction or mismatched-currency operation |
| `InvalidStateTransitionException` | Illegal payment state transition — carries `paymentId`, `from`, `to` |
| `DuplicateIdempotencyKeyException` | Idempotency key already exists — carries `idempotencyKey`, `existingPaymentId` |

All exceptions are **unchecked** (`RuntimeException`).

---

#### Unit Tests

| Test class | Coverage area |
|------------|---------------|
| `MoneyTest` | Construction, arithmetic, comparison, equality, immutability, toString, INR-specific scenarios |
| `PaymentStatusTest` | All legal/illegal transitions for each state, `isTerminal()` |
| `PaymentIntentTest` | Factory, full lifecycle paths, all invalid transitions, exception context, `updatedAt` refresh, INR lifecycle scenarios |

**INR test coverage includes:**
- `Money.of("999", "INR")` — whole rupee amounts
- `Money.of("1299.50", "INR")` — paise (sub-rupee) amounts
- INR arithmetic: addition with GST, subtraction from wallet balance
- INR vs USD mismatch — all operations throw `InvalidMoneyException`
- Full payment lifecycle (create → process → succeed/fail/cancel) with INR amounts
- High-value INR transaction (₹50,000)
- Amount preserved through all lifecycle transitions

Run:
```bash
cd services/payments-core
./gradlew test
```

---

### Phase 2 — Persistence Layer
**Status:** Not started

Planned: PaymentRepository (Postgres), migrations, Testcontainers integration tests.

---

### Phase 3 — Application + HTTP Layer
**Status:** Not started

Planned: Use cases (CreatePayment, ProcessPayment, etc.), HTTP handlers, DTOs.

---

## ledger-service (Scala)

**Status:** Not started — begins after Phase 3 of payments-core.

---

## api-gateway (Go)

**Status:** Not started — begins after ledger-service domain layer.

---

## webhook-service (Ruby)

**Status:** Not started — begins after event integration phase.
