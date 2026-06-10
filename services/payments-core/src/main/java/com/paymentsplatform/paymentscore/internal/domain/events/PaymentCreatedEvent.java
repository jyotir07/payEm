package com.paymentsplatform.paymentscore.internal.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreatedEvent(UUID paymentId, BigDecimal amount, String currency, Instant timestamp)
        implements PaymentEvent {
    @Override public String eventType() { return "PaymentCreated"; }
    @Override public String routingKey() { return "payment.created"; }
}
