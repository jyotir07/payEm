package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.application.ConfirmPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.ProcessPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidStateTransitionException;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfirmPaymentUseCaseTest {

    private InMemoryPaymentRepository repo;
    private ProcessPaymentUseCase process;
    private ConfirmPaymentUseCase confirm;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
        process = new ProcessPaymentUseCase(repo);
        confirm = new ConfirmPaymentUseCase(repo);
    }

    @Test
    void confirm_succeeded_movesToSucceeded() {
        PaymentIntent intent = givenProcessingIntent("confirm-ok-12345678");
        PaymentIntent result = confirm.execute(intent.getId(), ConfirmPaymentUseCase.Outcome.SUCCEEDED);
        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
    }

    @Test
    void confirm_failed_movesToFailed() {
        PaymentIntent intent = givenProcessingIntent("confirm-fail-1234567");
        PaymentIntent result = confirm.execute(intent.getId(), ConfirmPaymentUseCase.Outcome.FAILED);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void confirm_fromCreated_throwsInvalidStateTransition() {
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "confirm-too-early-123"
        );
        repo.save(intent);
        assertThrows(InvalidStateTransitionException.class,
                () -> confirm.execute(intent.getId(), ConfirmPaymentUseCase.Outcome.SUCCEEDED));
    }

    @Test
    void confirm_unknownId_throwsNotFound() {
        assertThrows(PaymentNotFoundException.class,
                () -> confirm.execute(UUID.randomUUID(), ConfirmPaymentUseCase.Outcome.SUCCEEDED));
    }

    @Test
    void confirm_nullOutcome_throwsIllegalArgument() {
        PaymentIntent intent = givenProcessingIntent("confirm-null-out-1234");
        assertThrows(IllegalArgumentException.class, () -> confirm.execute(intent.getId(), null));
    }

    private PaymentIntent givenProcessingIntent(String key) {
        PaymentIntent intent = PaymentIntent.create(Money.of(new BigDecimal("100"), "INR"), key);
        repo.save(intent);
        process.execute(intent.getId());
        return intent;
    }
}
