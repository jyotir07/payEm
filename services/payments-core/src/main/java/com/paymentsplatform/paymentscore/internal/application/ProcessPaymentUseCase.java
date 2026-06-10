package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Transitions a PaymentIntent from CREATED to PROCESSING and publishes
 * a PaymentProcessing event after the save commits.
 */
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public ProcessPaymentUseCase(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentIntent execute(UUID paymentId) {
        PaymentIntent intent = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        intent.startProcessing();
        paymentRepository.save(intent);
        eventPublisher.publish(PaymentEvent.processing(intent));
        return intent;
    }
}
