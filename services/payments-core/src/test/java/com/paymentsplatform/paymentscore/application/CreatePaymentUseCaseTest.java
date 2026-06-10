package com.paymentsplatform.paymentscore.application;

import com.paymentsplatform.paymentscore.internal.application.CreatePaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidMoneyException;
import com.paymentsplatform.paymentscore.internal.infrastructure.NoopPaymentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatePaymentUseCaseTest {

    private InMemoryPaymentRepository repo;
    private InMemoryIdempotencyStore store;
    private CreatePaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
        store = new InMemoryIdempotencyStore();
        useCase = new CreatePaymentUseCase(repo, store, new NoopPaymentEventPublisher());
    }

    @Test
    void create_persistsNewPaymentInCreatedState() {
        CreatePaymentUseCase.Result result = useCase.execute(
                new BigDecimal("1000.00"), "INR", "key-create-12345678"
        );

        assertTrue(result.isNew());
        assertNotNull(result.getPaymentIntent().getId());
        assertEquals(PaymentStatus.CREATED, result.getPaymentIntent().getStatus());
        assertEquals("INR", result.getPaymentIntent().getAmount().getCurrency());
        assertTrue(store.exists("key-create-12345678"));
    }

    @Test
    void create_withDuplicateIdempotencyKey_returnsExistingIntent() {
        CreatePaymentUseCase.Result first = useCase.execute(
                new BigDecimal("500.00"), "USD", "key-dup-12345678"
        );
        CreatePaymentUseCase.Result second = useCase.execute(
                new BigDecimal("9999.99"), "EUR", "key-dup-12345678"
        );

        assertTrue(first.isNew());
        assertFalse(second.isNew());
        assertSame(first.getPaymentIntent().getId(), second.getPaymentIntent().getId());
        // Original amount preserved — second request did NOT overwrite
        assertEquals(new BigDecimal("500.0000"), second.getPaymentIntent().getAmount().getAmount());
        assertEquals("USD", second.getPaymentIntent().getAmount().getCurrency());
    }

    @Test
    void create_withInvalidCurrency_throwsInvalidMoney() {
        assertThrows(InvalidMoneyException.class, () -> useCase.execute(
                new BigDecimal("100"), "INVALID", "key-bad-cur-12345"
        ));
    }

    @Test
    void create_withNegativeAmount_throwsInvalidMoney() {
        assertThrows(InvalidMoneyException.class, () -> useCase.execute(
                new BigDecimal("-1"), "INR", "key-neg-12345678"
        ));
    }

    @Test
    void create_withShortIdempotencyKey_throws() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new BigDecimal("100"), "INR", "short"
        ));
    }
}
