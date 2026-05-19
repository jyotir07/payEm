package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Transitions a PaymentIntent from CREATED to PROCESSING.
 *
 * Phase 6 will extend this to emit a PaymentProcessingEvent after save.
 */
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public ProcessPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentIntent execute(UUID paymentId) {
        PaymentIntent intent = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        intent.startProcessing();
        paymentRepository.save(intent);
        return intent;
    }
}
