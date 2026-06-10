package com.paymentsplatform.paymentscore.internal.infrastructure;

import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;

/**
 * Drops events on the floor. Used when no broker is configured (local dev without
 * RabbitMQ, unit tests that don't care about side effects, integration tests of
 * non-event paths).
 */
public final class NoopPaymentEventPublisher implements PaymentEventPublisher {
    @Override
    public void publish(PaymentEvent event) {
        // intentionally empty
    }
}
