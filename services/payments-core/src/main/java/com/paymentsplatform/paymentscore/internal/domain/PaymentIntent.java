package com.paymentsplatform.paymentscore.internal.domain;

import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core domain entity representing a payment intent.
 *
 * A PaymentIntent tracks a single payment's lifecycle from creation through
 * to a terminal state (SUCCEEDED, FAILED, or CANCELED).
 *
 * Design decisions:
 * - Created via a static factory — constructors are private.
 * - State is changed only through named transition methods (startProcessing, markSucceeded, etc.).
 * - No public setters — the entity enforces its own invariants.
 * - Each transition validates against PaymentStatus.canTransitionTo() and throws
 *   InvalidStateTransitionException if the transition is illegal.
 * - updatedAt is updated on every successful transition.
 */
public final class PaymentIntent {

    private final UUID id;
    private final Money amount;
    private final String idempotencyKey;
    private final Instant createdAt;

    private PaymentStatus status;
    private Instant updatedAt;

    private PaymentIntent(
            UUID id,
            Money amount,
            String idempotencyKey,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new PaymentIntent in the CREATED state.
     *
     * @param amount         the monetary amount — must not be null
     * @param idempotencyKey the unique idempotency key — must not be null or blank
     */
    public static PaymentIntent create(Money amount, String idempotencyKey) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be null or blank");
        }
        Instant now = Instant.now();
        return new PaymentIntent(
                UUID.randomUUID(),
                amount,
                idempotencyKey.trim(),
                PaymentStatus.CREATED,
                now,
                now
        );
    }

    /**
     * Transitions the payment from CREATED to PROCESSING.
     *
     * @throws InvalidStateTransitionException if the current status is not CREATED
     */
    public void startProcessing() {
        transition(PaymentStatus.PROCESSING);
    }

    /**
     * Transitions the payment from PROCESSING to SUCCEEDED.
     *
     * @throws InvalidStateTransitionException if the current status is not PROCESSING
     */
    public void markSucceeded() {
        transition(PaymentStatus.SUCCEEDED);
    }

    /**
     * Transitions the payment from PROCESSING to FAILED.
     *
     * @throws InvalidStateTransitionException if the current status is not PROCESSING
     */
    public void markFailed() {
        transition(PaymentStatus.FAILED);
    }

    /**
     * Transitions the payment to CANCELED.
     * Legal from CREATED only — cannot cancel a payment already PROCESSING or in a terminal state.
     *
     * @throws InvalidStateTransitionException if the current status does not allow cancellation
     */
    public void cancel() {
        transition(PaymentStatus.CANCELED);
    }

    private void transition(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStateTransitionException(id, status, next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Money getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentIntent)) return false;
        PaymentIntent that = (PaymentIntent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PaymentIntent{id=" + id + ", status=" + status + ", amount=" + amount + "}";
    }
}
