package com.paymentsplatform.paymentscore.internal.errors;

/**
 * Catalogue of all domain error types in payments-core.
 *
 * Domain exceptions are defined in the domain/exceptions package:
 * - InvalidMoneyException         — bad Money construction or currency mismatch
 * - InvalidStateTransitionException — illegal payment status transition
 * - DuplicateIdempotencyKeyException — idempotency key already exists
 *
 * This class will be extended in Phase 3 to map domain exceptions to HTTP
 * status codes for the transport layer (e.g. 409 for duplicate idempotency
 * key, 422 for invalid state transition).
 */
public final class PaymentErrors {
    private PaymentErrors() {}
}
