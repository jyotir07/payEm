package com.paymentsplatform.paymentscore.internal.domain.exceptions;

import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;

import java.util.UUID;

/**
 * Thrown when an illegal state transition is attempted on a PaymentIntent.
 *
 * Carries the payment ID and the from/to states for diagnostic purposes.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final UUID paymentId;
    private final PaymentStatus from;
    private final PaymentStatus to;

    public InvalidStateTransitionException(UUID paymentId, PaymentStatus from, PaymentStatus to) {
        super(String.format(
                "Invalid state transition for payment [%s]: cannot move from %s to %s",
                paymentId, from, to
        ));
        this.paymentId = paymentId;
        this.from = from;
        this.to = to;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getFrom() {
        return from;
    }

    public PaymentStatus getTo() {
        return to;
    }
}
