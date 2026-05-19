package com.paymentsplatform.paymentscore.internal.application;

import com.paymentsplatform.paymentscore.internal.domain.IdempotencyKey;
import com.paymentsplatform.paymentscore.internal.domain.IdempotencyStore;
import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Creates a new PaymentIntent.
 *
 * Idempotency contract: if the supplied key already exists, the existing PaymentIntent is
 * returned and no new row is written. The result object distinguishes the two cases via
 * {@link Result#isNew()} so the transport layer can choose the correct HTTP status.
 */
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;

    public CreatePaymentUseCase(PaymentRepository paymentRepository, IdempotencyStore idempotencyStore) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
    }

    public Result execute(BigDecimal amount, String currency, String idempotencyKey) {
        String key = IdempotencyKey.of(idempotencyKey).getValue();

        if (idempotencyStore.exists(key)) {
            Optional<PaymentIntent> existing = paymentRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                return Result.existing(existing.get());
            }
        }

        Money money = Money.of(amount, currency);
        PaymentIntent intent = PaymentIntent.create(money, key);
        paymentRepository.save(intent);
        idempotencyStore.save(key, intent.getId());
        return Result.created(intent);
    }

    public static final class Result {
        private final PaymentIntent paymentIntent;
        private final boolean isNew;

        private Result(PaymentIntent paymentIntent, boolean isNew) {
            this.paymentIntent = paymentIntent;
            this.isNew = isNew;
        }

        static Result created(PaymentIntent paymentIntent) {
            return new Result(paymentIntent, true);
        }

        static Result existing(PaymentIntent paymentIntent) {
            return new Result(paymentIntent, false);
        }

        public PaymentIntent getPaymentIntent() {
            return paymentIntent;
        }

        public boolean isNew() {
            return isNew;
        }
    }
}
