# api-gateway

Go service acting as the single entrypoint for external traffic.

## Responsibilities

- **Authentication** — Validate API keys or tokens and attach identity to context.
- **Rate limiting** — Enforce per-tenant or per-key request limits (429 when exceeded).
- **Request validation** — Validate body, query, and headers before forwarding.
- **Routing** — Route requests to payments-core, ledger-service, and webhook-service.
- **Context and request ID propagation** — Generate/forward `X-Request-ID` and propagate context to downstream services.

## Layout

- `cmd/api-gateway/` — Application entrypoint.
- `internal/api/` — HTTP handlers and router.
- `internal/middleware/` — Auth, rate limit, validation, request ID.
- `internal/clients/` — Stubs for downstream services.
- `internal/config/` — Runtime configuration.
- `internal/errors/` — Typed errors and HTTP mapping.
- `migrations/` — DB migrations if gateway persists state.
- `tests/` — Integration and service-level tests.

## Run locally

```bash
go run ./cmd/api-gateway
```

Default port: `8080`. See `internal/config` for env-based configuration.
