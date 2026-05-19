package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.application.CancelPaymentUseCase;
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

class CancelPaymentUseCaseTest {

    private InMemoryPaymentRepository repo;
    private CancelPaymentUseCase cancel;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
        cancel = new CancelPaymentUseCase(repo);
    }

    @Test
    void cancel_fromCreated_succeeds() {
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "cancel-ok-12345678"
        );
        repo.save(intent);

        PaymentIntent result = cancel.execute(intent.getId());
        assertEquals(PaymentStatus.CANCELED, result.getStatus());
    }

    @Test
    void cancel_fromProcessing_throwsInvalidStateTransition() {
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "cancel-too-late-12345"
        );
        repo.save(intent);
        new ProcessPaymentUseCase(repo).execute(intent.getId());

        assertThrows(InvalidStateTransitionException.class, () -> cancel.execute(intent.getId()));
    }

    @Test
    void cancel_missingPayment_throwsNotFound() {
        assertThrows(PaymentNotFoundException.class, () -> cancel.execute(UUID.randomUUID()));
    }
}
