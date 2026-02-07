# Architecture

High-level system design for the payments platform.

## Overview

- **API Gateway** — Single entrypoint; auth, rate limiting, validation, routing.
- **Payments Core** — Payment intent lifecycle, idempotency, state machines, event emission.
- **Ledger Service** — Double-entry accounting, balance invariants, immutable ledger.
- **Webhook Service** — Webhook delivery, signing, retry with backoff, dead-letter.

Traffic: Client to API Gateway to internal services. Events from Payments Core to Ledger and Webhook via message broker.
