package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.domain.IdempotencyStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, UUID> keys = new HashMap<>();

    @Override
    public boolean exists(String key) {
        return keys.containsKey(key);
    }

    @Override
    public void save(String key, UUID paymentId) {
        keys.put(key, paymentId);
    }
}
