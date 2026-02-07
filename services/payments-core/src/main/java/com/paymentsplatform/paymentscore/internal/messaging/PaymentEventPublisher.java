// Event publisher for payment lifecycle events. Emits events (e.g. payment.created,
// payment.succeeded) to message broker for downstream consumers (ledger, webhooks).
// Responsibility: Publish payment events to async pipeline.

package com.paymentsplatform.paymentscore.internal.messaging;
