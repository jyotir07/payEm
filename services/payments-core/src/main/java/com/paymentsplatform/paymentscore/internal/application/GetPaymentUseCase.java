package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Fetches a PaymentIntent by id. Backs the GET /payments/{id} endpoint.
 *
 * Kept in the application layer so the transport layer holds no repository reference,
 * even for read operations.
 */
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentIntent execute(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
