# auth

Shared authentication helpers used by api-gateway and other services that validate identity.

## Responsibility

- **API key validation** — Verify format and lookup (e.g. in shared store).
- **Token / JWT parsing** — Extract tenant and user from tokens.
- **Context helpers** — Attach identity to request context in a consistent way across languages (reference impl or spec).
