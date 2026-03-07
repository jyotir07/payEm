package com.paymentsplatform.paymentscore.internal.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for persisting and retrieving PaymentIntents.
 *
 * Defined in the domain layer so that use cases depend only on this abstraction.
 * The concrete implementation lives in the infrastructure layer (PostgresPaymentRepository).
 *
 * No framework annotations — this is pure Java.
 */
public interface PaymentRepository {

    /**
     * Persists a PaymentIntent. Inserts on first call; updates status and updatedAt on subsequent calls.
     */
    void save(PaymentIntent payment);

    /**
     * Finds a PaymentIntent by its unique identifier.
     */
    Optional<PaymentIntent> findById(UUID id);

    /**
     * Finds a PaymentIntent by its idempotency key.
     * Used to return an existing intent when a duplicate request arrives.
     */
    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);
}
