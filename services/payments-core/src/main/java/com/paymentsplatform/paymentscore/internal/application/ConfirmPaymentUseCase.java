package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Confirms the result of a PROCESSING payment — moves it to SUCCEEDED or FAILED.
 */
public class ConfirmPaymentUseCase {

    public enum Outcome { SUCCEEDED, FAILED }

    private final PaymentRepository paymentRepository;

    public ConfirmPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentIntent execute(UUID paymentId, Outcome outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome must not be null");
        }
        PaymentIntent intent = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        switch (outcome) {
            case SUCCEEDED -> intent.markSucceeded();
            case FAILED -> intent.markFailed();
        }
        paymentRepository.save(intent);
        return intent;
    }
}
