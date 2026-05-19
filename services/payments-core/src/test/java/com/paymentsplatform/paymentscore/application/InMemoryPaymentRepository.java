package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory PaymentRepository for unit tests. Mirrors the upsert semantics of the
 * Postgres implementation (later save() calls overwrite the stored snapshot).
 */
final class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, PaymentIntent> byId = new HashMap<>();
    private final Map<String, UUID> byKey = new HashMap<>();

    @Override
    public void save(PaymentIntent payment) {
        byId.put(payment.getId(), payment);
        byKey.putIfAbsent(payment.getIdempotencyKey(), payment.getId());
    }

    @Override
    public Optional<PaymentIntent> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey) {
        UUID id = byKey.get(idempotencyKey);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }
}
