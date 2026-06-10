package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;

import java.util.UUID;

/**
 * Cancels a PaymentIntent. Legal only from CREATED.
 * Publishes a PaymentCanceled event after the save commits.
 */
public class CancelPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public CancelPaymentUseCase(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentIntent execute(UUID paymentId) {
        PaymentIntent intent = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        intent.cancel();
        paymentRepository.save(intent);
        eventPublisher.publish(PaymentEvent.canceled(intent));
        return intent;
    }
}
