# ledger-service

Scala service implementing double-entry accounting, balance invariants, and immutable ledger records.

## Responsibilities

- **Double-entry accounting** — Every transaction has equal debits and credits; no orphan entries.
- **Balance invariants** — Account balance equals sum of entry effects; invariants checked on post.
- **Immutable ledger records** — Ledger entries are append-only; no updates or deletes.

## Layout

- `src/main/scala/.../Main.scala` — Entrypoint.
- `internal/api/` — HTTP/gRPC handlers.
- `internal/domain/` — Account, LedgerEntry, and Invariants (enforcement comments).
- `internal/persistence/` — Ledger repository and balance store.
- `internal/messaging/` — Event consumption/publishing.
- `internal/config/` — Runtime configuration.
- `internal/errors/` — Typed errors.
- `migrations/` — DB schema migrations.
- `tests/` — Integration and service-level tests.

## Run locally

```bash
sbt run
```

Default port: `8082`. See `internal/config` for env-based configuration.
See `docs/ledger-invariants.md` for accounting guarantees.
