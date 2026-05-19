package com.paymentsplatform.paymentscore.internal.domain.exceptions;

import java.util.UUID;

/**
 * Thrown when an operation references a PaymentIntent that does not exist.
 *
 * The transport layer maps this to HTTP 404.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final UUID paymentId;

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
        this.paymentId = paymentId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }
}
