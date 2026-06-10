package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Confirms the result of a PROCESSING payment — moves it to SUCCEEDED or FAILED
 * and publishes the matching event after the save commits.
 */
public class ConfirmPaymentUseCase {

    public enum Outcome { SUCCEEDED, FAILED }

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public ConfirmPaymentUseCase(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
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
        switch (outcome) {
            case SUCCEEDED -> eventPublisher.publish(PaymentEvent.succeeded(intent));
            case FAILED -> eventPublisher.publish(PaymentEvent.failed(intent));
        }
        return intent;
    }
}
