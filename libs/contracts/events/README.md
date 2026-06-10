# Payment Domain Events

Shared event contract between `payments-core` (publisher) and downstream consumers
(`ledger-service`, `webhook-service`). These are the JSON wire formats; the language-
specific event classes in each service must match them byte-for-byte.

## Transport

- Broker: RabbitMQ
- Exchange: `payments.events` (type: `topic`, durable)
- Routing keys:
  - `payment.created`
  - `payment.processing`
  - `payment.succeeded`
  - `payment.failed`
  - `payment.canceled`
- Messages are published as `application/json`, UTF-8, `persistent` delivery mode.

Consumers declare their own queue and bind it to the routing keys they care about.
The ledger consumer binds queue `ledger.payment-events` to `payment.succeeded`.

## Envelope

Every event shares the same five top-level fields:

| Field           | Type    | Notes                                                   |
|-----------------|---------|---------------------------------------------------------|
| `eventType`     | string  | One of `PaymentCreated`, `PaymentProcessing`, `PaymentSucceeded`, `PaymentFailed`, `PaymentCanceled`. |
| `schemaVersion` | integer | Starts at `1`. Bump on breaking change.                 |
| `paymentId`     | string  | UUID of the originating `PaymentIntent`.                |
| `amount`        | string  | Decimal serialised as a string to preserve precision. |
| `currency`      | string  | ISO 4217 (e.g. `INR`, `USD`).                           |
| `timestamp`     | string  | RFC 3339 / ISO 8601, UTC, e.g. `2026-06-10T12:34:56Z`.  |

`amount` is a string, not a JSON number, because JSON `number` does not have a
specified precision and some parsers silently coerce to `double`. Consumers must
parse it via `BigDecimal` / `BigDecimal.exact`.

## Delivery semantics

`payments-core` publishes after the database commit succeeds. On publish failure the
event is dropped and the failure is logged — the DB write is not rolled back. This
means delivery is **at-most-once** today; we accept the gap because every consumer
of these events is **idempotent** on `paymentId` (ledger looks up
`findEntriesByPayment`; webhook will dedupe on delivery id). A transactional outbox
is a future hardening step.

## Schema evolution

- Adding optional fields is non-breaking; consumers must ignore unknown fields.
- Removing or renaming a field requires bumping `schemaVersion` and a coordinated
  consumer update.
- Never change the meaning of a field in place.
