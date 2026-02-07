# idempotency

Shared idempotency utilities and key handling used by payments-core and any service that needs idempotent operations.

## Responsibility

- **Key format** — Standard idempotency key format (e.g. header name, key rules).
- **Response caching contract** — How to store and return cached responses.
- **Reference or spec** for implementations in Java and other languages.
