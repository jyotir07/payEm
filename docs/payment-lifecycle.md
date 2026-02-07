# Payment Lifecycle

Payment state transitions for payments-core.

## States

- **created** — Intent created; awaiting processing.
- **processing** — Payment in progress.
- **succeeded** — Completed successfully.
- **failed** — Failed (e.g. insufficient funds).
- **canceled** — Canceled by user or system.

## Transitions

Enforced by state machine in payments-core. Invalid transitions are rejected.

## Idempotency

Duplicate requests with same idempotency key return cached response. See libs/idempotency.
