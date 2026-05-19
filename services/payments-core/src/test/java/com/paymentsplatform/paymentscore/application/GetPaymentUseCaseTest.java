package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.application.GetPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetPaymentUseCaseTest {

    @Test
    void get_returnsPersistedIntent() {
        InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
        PaymentIntent intent = PaymentIntent.create(
                Money.of(new BigDecimal("100"), "INR"), "get-happy-1234567890"
        );
        repo.save(intent);

        PaymentIntent fetched = new GetPaymentUseCase(repo).execute(intent.getId());
        assertEquals(intent.getId(), fetched.getId());
    }

    @Test
    void get_missing_throwsNotFound() {
        InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
        assertThrows(PaymentNotFoundException.class,
                () -> new GetPaymentUseCase(repo).execute(UUID.randomUUID()));
    }
}
