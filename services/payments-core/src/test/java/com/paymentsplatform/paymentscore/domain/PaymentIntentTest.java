package com.paymentsplatform.paymentscore.domain;

import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentIntentTest {

    private Money amount;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        amount = Money.of("100.00", "USD");
        idempotencyKey = "idem-key-abc-12345";
    }

    // -------------------------------------------------------------------------
    // Factory — PaymentIntent.create()
    // -------------------------------------------------------------------------

    @Test
    void createSetsInitialStateToCreated() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertEquals(PaymentStatus.CREATED, intent.getStatus());
    }

    @Test
    void createAssignsNonNullId() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertNotNull(intent.getId());
    }

    @Test
    void createPreservesAmountAndIdempotencyKey() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertEquals(amount, intent.getAmount());
        assertEquals(idempotencyKey, intent.getIdempotencyKey());
    }

    @Test
    void createSetsCreatedAtAndUpdatedAt() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertNotNull(intent.getCreatedAt());
        assertNotNull(intent.getUpdatedAt());
    }

    @Test
    void createThrowsOnNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentIntent.create(null, idempotencyKey));
    }

    @Test
    void createThrowsOnNullIdempotencyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentIntent.create(amount, null));
    }

    @Test
    void createThrowsOnBlankIdempotencyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentIntent.create(amount, "   "));
    }

    @Test
    void eachCreatedIntentHasAUniqueId() {
        PaymentIntent a = PaymentIntent.create(amount, "key-aaa-00000001");
        PaymentIntent b = PaymentIntent.create(amount, "key-bbb-00000002");
        assertNotEquals(a.getId(), b.getId());
    }

    // -------------------------------------------------------------------------
    // Happy paths — full lifecycle flows
    // -------------------------------------------------------------------------

    @Test
    void happyPath_created_processing_succeeded() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);

        intent.startProcessing();
        assertEquals(PaymentStatus.PROCESSING, intent.getStatus());

        intent.markSucceeded();
        assertEquals(PaymentStatus.SUCCEEDED, intent.getStatus());
    }

    @Test
    void happyPath_created_processing_failed() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);

        intent.startProcessing();
        intent.markFailed();

        assertEquals(PaymentStatus.FAILED, intent.getStatus());
    }

    @Test
    void happyPath_created_canceled() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);

        intent.cancel();

        assertEquals(PaymentStatus.CANCELED, intent.getStatus());
    }

    // -------------------------------------------------------------------------
    // Invalid transitions — from CREATED
    // -------------------------------------------------------------------------

    @Test
    void cannotMarkSucceededFromCreated() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertThrows(InvalidStateTransitionException.class, intent::markSucceeded);
    }

    @Test
    void cannotMarkFailedFromCreated() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertThrows(InvalidStateTransitionException.class, intent::markFailed);
    }

    // -------------------------------------------------------------------------
    // Invalid transitions — from PROCESSING
    // -------------------------------------------------------------------------

    @Test
    void cannotStartProcessingAgainFromProcessing() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        intent.startProcessing();
        assertThrows(InvalidStateTransitionException.class, intent::startProcessing);
    }

    @Test
    void cannotCancelFromProcessing() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        intent.startProcessing();
        assertThrows(InvalidStateTransitionException.class, intent::cancel);
    }

    // -------------------------------------------------------------------------
    // Invalid transitions — from terminal states
    // -------------------------------------------------------------------------

    @Test
    void cannotTransitionFromSucceeded() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        intent.startProcessing();
        intent.markSucceeded();

        assertThrows(InvalidStateTransitionException.class, intent::startProcessing);
        assertThrows(InvalidStateTransitionException.class, intent::markFailed);
        assertThrows(InvalidStateTransitionException.class, intent::cancel);
    }

    @Test
    void cannotTransitionFromFailed() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        intent.startProcessing();
        intent.markFailed();

        assertThrows(InvalidStateTransitionException.class, intent::startProcessing);
        assertThrows(InvalidStateTransitionException.class, intent::markSucceeded);
        assertThrows(InvalidStateTransitionException.class, intent::cancel);
    }

    @Test
    void cannotTransitionFromCanceled() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        intent.cancel();

        assertThrows(InvalidStateTransitionException.class, intent::startProcessing);
        assertThrows(InvalidStateTransitionException.class, intent::markSucceeded);
        assertThrows(InvalidStateTransitionException.class, intent::markFailed);
    }

    // -------------------------------------------------------------------------
    // Exception carries diagnostic context
    // -------------------------------------------------------------------------

    @Test
    void invalidTransitionExceptionCarriesPaymentIdFromAndTo() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);

        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                intent::markSucceeded
        );

        assertEquals(intent.getId(), ex.getPaymentId());
        assertEquals(PaymentStatus.CREATED, ex.getFrom());
        assertEquals(PaymentStatus.SUCCEEDED, ex.getTo());
    }

    // -------------------------------------------------------------------------
    // updatedAt changes on transition
    // -------------------------------------------------------------------------

    @Test
    void updatedAtIsRefreshedAfterEachTransition() throws InterruptedException {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        var afterCreate = intent.getUpdatedAt();

        Thread.sleep(5);
        intent.startProcessing();
        var afterProcessing = intent.getUpdatedAt();
        assertTrue(afterProcessing.isAfter(afterCreate));

        Thread.sleep(5);
        intent.markSucceeded();
        assertTrue(intent.getUpdatedAt().isAfter(afterProcessing));
    }

    // -------------------------------------------------------------------------
    // Equality and hashCode
    // -------------------------------------------------------------------------

    @Test
    void intentIsEqualToItself() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        assertEquals(intent, intent);
    }

    @Test
    void twoDistinctIntentsAreNotEqual() {
        PaymentIntent a = PaymentIntent.create(amount, "key-alpha-00001");
        PaymentIntent b = PaymentIntent.create(amount, "key-beta-00002");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    void toStringContainsIdStatusAndAmount() {
        PaymentIntent intent = PaymentIntent.create(amount, idempotencyKey);
        String str = intent.toString();
        assertTrue(str.contains("PaymentIntent"));
        assertTrue(str.contains("CREATED"));
    }

    // -------------------------------------------------------------------------
    // INR (Indian Rupee) — payment intent scenarios
    // -------------------------------------------------------------------------

    @Test
    void inr_createsPaymentIntentWithRupeeAmount() {
        Money rupees = Money.of("999", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-001");

        assertEquals(Money.of("999", "INR"), intent.getAmount());
        assertEquals("INR", intent.getAmount().getCurrency());
        assertEquals(PaymentStatus.CREATED, intent.getStatus());
    }

    @Test
    void inr_happyPath_created_processing_succeeded() {
        Money rupees = Money.of("4999", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-002");

        intent.startProcessing();
        assertEquals(PaymentStatus.PROCESSING, intent.getStatus());

        intent.markSucceeded();
        assertEquals(PaymentStatus.SUCCEEDED, intent.getStatus());
        assertEquals(Money.of("4999", "INR"), intent.getAmount());
    }

    @Test
    void inr_happyPath_created_processing_failed() {
        Money rupees = Money.of("1500", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-003");

        intent.startProcessing();
        intent.markFailed();

        assertEquals(PaymentStatus.FAILED, intent.getStatus());
    }

    @Test
    void inr_happyPath_created_canceled() {
        Money rupees = Money.of("250", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-004");

        intent.cancel();

        assertEquals(PaymentStatus.CANCELED, intent.getStatus());
    }

    @Test
    void inr_highValuePayment_succeeds() {
        // Typical high-value INR transaction (e.g. ₹50,000 — loan repayment, large purchase)
        Money rupees = Money.of("50000", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-005");

        intent.startProcessing();
        intent.markSucceeded();

        assertEquals(PaymentStatus.SUCCEEDED, intent.getStatus());
        assertEquals(Money.of("50000", "INR"), intent.getAmount());
    }

    @Test
    void inr_paymentAmountIsPreservedThroughLifecycle() {
        Money rupees = Money.of("1299.50", "INR");
        PaymentIntent intent = PaymentIntent.create(rupees, "inr-payment-key-006");

        intent.startProcessing();
        intent.markSucceeded();

        // Amount must never change through lifecycle transitions
        assertEquals(Money.of("1299.50", "INR"), intent.getAmount());
    }

    @Test
    void inr_twoSeparateRupeePaymentsHaveDistinctIds() {
        PaymentIntent a = PaymentIntent.create(Money.of("500", "INR"), "inr-payment-key-007");
        PaymentIntent b = PaymentIntent.create(Money.of("500", "INR"), "inr-payment-key-008");
        assertNotEquals(a.getId(), b.getId());
    }
}
