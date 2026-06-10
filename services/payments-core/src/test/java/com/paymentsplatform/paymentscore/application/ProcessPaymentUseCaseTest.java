package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.application.ProcessPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidStateTransitionException;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;
import com.paymentsplatform.paymentscore.internal.infrastructure.NoopPaymentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessPaymentUseCaseTest {

    private InMemoryPaymentRepository repo;
    private ProcessPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
        useCase = new ProcessPaymentUseCase(repo, new NoopPaymentEventPublisher());
    }

    @Test
    void process_transitionsCreatedToProcessing() {
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "process-happy-1234567"
        );
        repo.save(intent);

        PaymentIntent result = useCase.execute(intent.getId());

        assertEquals(PaymentStatus.PROCESSING, result.getStatus());
        assertEquals(PaymentStatus.PROCESSING, repo.findById(intent.getId()).orElseThrow().getStatus());
    }

    @Test
    void process_missingPayment_throwsNotFound() {
        assertThrows(PaymentNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    @Test
    void process_alreadyProcessing_throwsInvalidStateTransition() {
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "process-twice-1234567"
        );
        repo.save(intent);
        useCase.execute(intent.getId());

        assertThrows(InvalidStateTransitionException.class, () -> useCase.execute(intent.getId()));
    }
}
