package com.paymentsplatform.paymentscore.internal.domain;

import java.util.Objects;

/**
 * Value object representing an idempotency key.
 *
 * An idempotency key is a client-supplied string that uniquely identifies a request.
 * When the same key is submitted more than once, the system must return the result
 * of the first successful request rather than processing it again.
 *
 * Constraints:
 * - Must not be null or blank.
 * - Must be between 8 and 255 characters.
 */
public final class IdempotencyKey {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 255;

    private final String value;

    private IdempotencyKey(String value) {
        this.value = value;
    }

    public static IdempotencyKey of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be null or blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency key must be at least " + MIN_LENGTH + " characters, got: " + trimmed.length()
            );
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency key must not exceed " + MAX_LENGTH + " characters, got: " + trimmed.length()
            );
        }
        return new IdempotencyKey(trimmed);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyKey)) return false;
        IdempotencyKey that = (IdempotencyKey) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "IdempotencyKey{" + value + "}";
    }
}
