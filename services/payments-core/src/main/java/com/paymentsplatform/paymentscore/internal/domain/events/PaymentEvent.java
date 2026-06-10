package com.paymentsplatform.paymentscore.internal.domain.events;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for every payment domain event. Implementations are records
 * (immutable, value-based). Each event carries the data described in
 * libs/contracts/events/README.md.
 */
public sealed interface PaymentEvent
        permits PaymentCreatedEvent,
                PaymentProcessingEvent,
                PaymentSucceededEvent,
                PaymentFailedEvent,
                PaymentCanceledEvent {

    int SCHEMA_VERSION = 1;

    String eventType();

    UUID paymentId();

    BigDecimal amount();

    String currency();

    Instant timestamp();

    default int schemaVersion() {
        return SCHEMA_VERSION;
    }

    /** Routing key used on the {@code payments.events} topic exchange. */
    String routingKey();

    static PaymentCreatedEvent created(PaymentIntent intent) {
        return new PaymentCreatedEvent(
                intent.getId(), intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(), intent.getUpdatedAt());
    }

    static PaymentProcessingEvent processing(PaymentIntent intent) {
        return new PaymentProcessingEvent(
                intent.getId(), intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(), intent.getUpdatedAt());
    }

    static PaymentSucceededEvent succeeded(PaymentIntent intent) {
        return new PaymentSucceededEvent(
                intent.getId(), intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(), intent.getUpdatedAt());
    }

    static PaymentFailedEvent failed(PaymentIntent intent) {
        return new PaymentFailedEvent(
                intent.getId(), intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(), intent.getUpdatedAt());
    }

    static PaymentCanceledEvent canceled(PaymentIntent intent) {
        return new PaymentCanceledEvent(
                intent.getId(), intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(), intent.getUpdatedAt());
    }
}
