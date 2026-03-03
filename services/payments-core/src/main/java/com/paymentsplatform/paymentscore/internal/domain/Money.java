package com.paymentsplatform.paymentscore.internal.domain;

import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidMoneyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount with an ISO 4217 currency.
 *
 * Rules:
 * - Amount must be non-negative.
 * - Currency must be a 3-letter ISO 4217 code (e.g. "USD", "EUR").
 * - All arithmetic operations return new Money instances — this object is never mutated.
 * - Operations across different currencies always throw InvalidMoneyException.
 * - Amount is stored at 4 decimal places of precision.
 */
public final class Money {

    private static final int SCALE = 4;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new InvalidMoneyException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Amount must not be negative, got: " + amount);
        }
        if (currency == null || currency.isBlank()) {
            throw new InvalidMoneyException("Currency must not be null or blank");
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new InvalidMoneyException(
                    "Currency must be a 3-letter ISO 4217 code, got: " + currency
            );
        }
        return new Money(amount, normalized);
    }

    public static Money of(String amount, String currency) {
        if (amount == null) {
            throw new InvalidMoneyException("Amount must not be null");
        }
        try {
            return of(new BigDecimal(amount), currency);
        } catch (NumberFormatException e) {
            throw new InvalidMoneyException("Invalid amount format: " + amount);
        }
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException(
                    "Subtraction would result in a negative amount: " + this + " - " + other
            );
        }
        return new Money(result, this.currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new InvalidMoneyException("Cannot operate with a null Money value");
        }
        if (!this.currency.equals(other.currency)) {
            throw new InvalidMoneyException(
                    "Currency mismatch: cannot operate on " + this.currency + " and " + other.currency
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money other = (Money) o;
        return amount.compareTo(other.amount) == 0 && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
