# payments-core

Java service implementing payment intent lifecycle, idempotency, and event emission.

## Responsibilities

- **Payment intent lifecycle** — Create, process, and transition payment intents through defined states.
- **Idempotency handling** — Store and honor idempotency keys so duplicate requests return cached results.
- **State machines** — Enforce valid state transitions (e.g. created → processing → succeeded).
- **Emitting payment events** — Publish events to message broker for ledger and webhook consumers.

## Layout

- `src/main/java/.../Application.java` — Entrypoint.
- `internal/api/` — HTTP/gRPC handlers.
- `internal/domain/` — Payment intent, state machine, idempotency key models.
- `internal/persistence/` — Repositories and idempotency store.
- `internal/messaging/` — Payment event publisher.
- `internal/config/` — Runtime configuration.
- `internal/errors/` — Typed errors.
- `migrations/` — DB schema migrations.
- `tests/` — Integration and service-level tests.

## Run locally

```bash
./gradlew run
```

Default port: `8081`. See `internal/config` for env-based configuration.
