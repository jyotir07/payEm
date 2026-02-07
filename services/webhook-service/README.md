# webhook-service

Ruby service responsible for webhook delivery, payload signing, retry with backoff, and dead-letter handling.

## Responsibilities

- **Webhook delivery** — HTTP POST to customer endpoints with signed payloads.
- **Payload signing** — Compute and attach signature (e.g. HMAC) so recipients can verify.
- **Retry with backoff** — Retry failed deliveries with exponential backoff and jitter.
- **Dead-letter handling** — Persist or notify when delivery fails after max retries.

## Layout

- `app.rb` / `config.ru` — Application entrypoint.
- `internal/api/` — HTTP handlers.
- `internal/domain/` — Webhook delivery and endpoint models.
- `internal/signing/` — Signer and verifier for payloads.
- `internal/retry/` — Backoff, delivery job, dead-letter.
- `internal/persistence/` — Delivery store.
- `internal/messaging/` — Event consumer.
- `internal/config/` — Runtime configuration.
- `internal/errors/` — Typed errors.
- `migrations/` — DB schema migrations.
- `tests/` — Integration and service-level tests.

## Run locally

```bash
bundle install
bundle exec puma -p 9292
```

Default port: `9292`. See `internal/config` for env-based configuration.
