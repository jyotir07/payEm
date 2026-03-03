package com.paymentsplatform.paymentscore.internal.domain.exceptions;

import java.util.UUID;

/**
 * Thrown when a create-payment request arrives with an idempotency key
 * that already exists in the system.
 *
 * The caller should respond by returning the existing PaymentIntent
 * rather than creating a new one.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    private final String idempotencyKey;
    private final UUID existingPaymentId;

    public DuplicateIdempotencyKeyException(String idempotencyKey, UUID existingPaymentId) {
        super(String.format(
                "Idempotency key '%s' already exists for payment [%s]",
                idempotencyKey, existingPaymentId
        ));
        this.idempotencyKey = idempotencyKey;
        this.existingPaymentId = existingPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getExistingPaymentId() {
        return existingPaymentId;
    }
}
