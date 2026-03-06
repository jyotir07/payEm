package com.paymentsplatform.paymentscore.internal.domain;

import java.util.UUID;

/**
 * Port (interface) for idempotency key tracking.
 *
 * Defined in the domain layer so use cases can check for duplicate requests
 * without depending on any infrastructure details.
 *
 * The concrete implementation lives in the infrastructure layer (PostgresIdempotencyStore).
 */
public interface IdempotencyStore {

    /**
     * Returns true if a payment with this idempotency key has already been created.
     */
    boolean exists(String key);

    /**
     * Records the association between an idempotency key and a payment ID.
     * Called after the PaymentIntent has been persisted via PaymentRepository.
     */
    void save(String key, UUID paymentId);
}
