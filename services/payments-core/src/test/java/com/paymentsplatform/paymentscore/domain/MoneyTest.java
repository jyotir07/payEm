package com.paymentsplatform.paymentscore.domain;

import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidMoneyException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    // -------------------------------------------------------------------------
    // Construction — valid inputs
    // -------------------------------------------------------------------------

    @Test
    void createsMoneyWithValidBigDecimalAndCurrency() {
        Money money = Money.of(new BigDecimal("10.00"), "USD");
        assertEquals(new BigDecimal("10.0000"), money.getAmount());
        assertEquals("USD", money.getCurrency());
    }

    @Test
    void normalizesCurrencyToUppercase() {
        Money money = Money.of(new BigDecimal("5.00"), "usd");
        assertEquals("USD", money.getCurrency());
    }

    @Test
    void allowsZeroAmount() {
        Money money = Money.of(BigDecimal.ZERO, "USD");
        assertTrue(money.isZero());
    }

    @Test
    void createsFromStringAmount() {
        Money money = Money.of("25.50", "EUR");
        assertEquals(new BigDecimal("25.5000"), money.getAmount());
        assertEquals("EUR", money.getCurrency());
    }

    // -------------------------------------------------------------------------
    // Construction — invalid inputs
    // -------------------------------------------------------------------------

    @Test
    void throwsOnNullBigDecimalAmount() {
        assertThrows(InvalidMoneyException.class, () -> Money.of((BigDecimal) null, "USD"));
    }

    @Test
    void throwsOnNegativeAmount() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("-0.01"), "USD"));
    }

    @Test
    void throwsOnNullCurrency() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("10.00"), null));
    }

    @Test
    void throwsOnBlankCurrency() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("10.00"), "   "));
    }

    @Test
    void throwsOnTwoLetterCurrency() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("10.00"), "US"));
    }

    @Test
    void throwsOnFourLetterCurrency() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("10.00"), "USDD"));
    }

    @Test
    void throwsOnInvalidStringAmount() {
        assertThrows(InvalidMoneyException.class, () -> Money.of("not-a-number", "USD"));
    }

    @Test
    void throwsOnNullStringAmount() {
        assertThrows(InvalidMoneyException.class, () -> Money.of((String) null, "USD"));
    }

    // -------------------------------------------------------------------------
    // Arithmetic — add
    // -------------------------------------------------------------------------

    @Test
    void addsTwoMoneyValuesOfSameCurrency() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("5.50", "USD");
        assertEquals(Money.of("15.50", "USD"), a.add(b));
    }

    @Test
    void addZeroReturnsEquivalentValue() {
        Money a = Money.of("10.00", "USD");
        Money zero = Money.of("0", "USD");
        assertEquals(a, a.add(zero));
    }

    @Test
    void throwsOnAddWithDifferentCurrency() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("5.00", "EUR");
        assertThrows(InvalidMoneyException.class, () -> usd.add(eur));
    }

    // -------------------------------------------------------------------------
    // Arithmetic — subtract
    // -------------------------------------------------------------------------

    @Test
    void subtractsTwoMoneyValuesOfSameCurrency() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("3.00", "USD");
        assertEquals(Money.of("7.00", "USD"), a.subtract(b));
    }

    @Test
    void subtractSameValueProducesZero() {
        Money a = Money.of("10.00", "USD");
        assertTrue(a.subtract(a).isZero());
    }

    @Test
    void throwsOnSubtractThatWouldProduceNegativeResult() {
        Money a = Money.of("5.00", "USD");
        Money b = Money.of("10.00", "USD");
        assertThrows(InvalidMoneyException.class, () -> a.subtract(b));
    }

    @Test
    void throwsOnSubtractWithDifferentCurrency() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("5.00", "EUR");
        assertThrows(InvalidMoneyException.class, () -> usd.subtract(eur));
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    @Test
    void isGreaterThanReturnsTrueWhenLarger() {
        assertTrue(Money.of("10.00", "USD").isGreaterThan(Money.of("5.00", "USD")));
    }

    @Test
    void isGreaterThanReturnsFalseWhenEqual() {
        assertFalse(Money.of("10.00", "USD").isGreaterThan(Money.of("10.00", "USD")));
    }

    @Test
    void isGreaterThanReturnsFalseWhenSmaller() {
        assertFalse(Money.of("3.00", "USD").isGreaterThan(Money.of("10.00", "USD")));
    }

    @Test
    void isGreaterThanThrowsOnCurrencyMismatch() {
        assertThrows(InvalidMoneyException.class,
                () -> Money.of("10.00", "USD").isGreaterThan(Money.of("5.00", "EUR")));
    }

    // -------------------------------------------------------------------------
    // Equality and hashCode
    // -------------------------------------------------------------------------

    @Test
    void twoInstancesWithSameValueAndCurrencyAreEqual() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("10.0000", "USD");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void instancesWithDifferentAmountsAreNotEqual() {
        assertNotEquals(Money.of("10.00", "USD"), Money.of("11.00", "USD"));
    }

    @Test
    void instancesWithDifferentCurrenciesAreNotEqual() {
        assertNotEquals(Money.of("10.00", "USD"), Money.of("10.00", "EUR"));
    }

    // -------------------------------------------------------------------------
    // Immutability
    // -------------------------------------------------------------------------

    @Test
    void addDoesNotMutateEitherOperand() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("5.00", "USD");
        a.add(b);
        assertEquals(Money.of("10.00", "USD"), a);
        assertEquals(Money.of("5.00", "USD"), b);
    }

    @Test
    void subtractDoesNotMutateEitherOperand() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("3.00", "USD");
        a.subtract(b);
        assertEquals(Money.of("10.00", "USD"), a);
        assertEquals(Money.of("3.00", "USD"), b);
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    void toStringContainsCurrencyAndAmount() {
        String str = Money.of("99.99", "USD").toString();
        assertTrue(str.contains("USD"));
        assertTrue(str.contains("99.99"));
    }

    // -------------------------------------------------------------------------
    // INR (Indian Rupee) — currency-specific scenarios
    // -------------------------------------------------------------------------

    @Test
    void createsInrWithWholeRupeeAmount() {
        Money money = Money.of("500", "INR");
        assertEquals("INR", money.getCurrency());
        assertEquals(new BigDecimal("500.0000"), money.getAmount());
    }

    @Test
    void createsInrWithPaiseAmount() {
        // INR supports paise (1 rupee = 100 paise), stored as decimals
        Money money = Money.of("999.50", "INR");
        assertEquals("INR", money.getCurrency());
        assertEquals(new BigDecimal("999.5000"), money.getAmount());
    }

    @Test
    void inrLowercaseCodeIsNormalized() {
        Money money = Money.of("100", "inr");
        assertEquals("INR", money.getCurrency());
    }

    @Test
    void addsInrAmounts() {
        Money base    = Money.of("1000", "INR");
        Money gst     = Money.of("180",  "INR");   // 18% GST on ₹1000
        Money total   = base.add(gst);
        assertEquals(Money.of("1180", "INR"), total);
    }

    @Test
    void subtractsInrAmounts() {
        Money wallet  = Money.of("5000", "INR");
        Money payment = Money.of("1299", "INR");
        Money balance = wallet.subtract(payment);
        assertEquals(Money.of("3701", "INR"), balance);
    }

    @Test
    void inrIsGreaterThanSmallerInrAmount() {
        assertTrue(Money.of("10000", "INR").isGreaterThan(Money.of("9999", "INR")));
    }

    @Test
    void inrAndUsdAreNotEqual() {
        // Same numeric value, different currency — must not be equal
        assertNotEquals(Money.of("100", "INR"), Money.of("100", "USD"));
    }

    @Test
    void throwsWhenAddingInrAndUsd() {
        Money rupees = Money.of("500", "INR");
        Money dollars = Money.of("10", "USD");
        assertThrows(InvalidMoneyException.class, () -> rupees.add(dollars));
    }

    @Test
    void throwsWhenSubtractingUsdFromInr() {
        Money rupees  = Money.of("500", "INR");
        Money dollars = Money.of("10",  "USD");
        assertThrows(InvalidMoneyException.class, () -> rupees.subtract(dollars));
    }

    @Test
    void throwsWhenComparingInrWithUsd() {
        Money rupees  = Money.of("1000", "INR");
        Money dollars = Money.of("12",   "USD");
        assertThrows(InvalidMoneyException.class, () -> rupees.isGreaterThan(dollars));
    }

    @Test
    void inrToStringContainsInrAndAmount() {
        String str = Money.of("4999", "INR").toString();
        assertTrue(str.contains("INR"));
        assertTrue(str.contains("4999"));
    }
}
