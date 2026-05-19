package com.paymentsplatform.paymentscore.internal.transport.dto;

import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound DTO for payment resources. Maps the domain entity onto the wire format
 * — never serialise PaymentIntent directly.
 */
public record PaymentResponse(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(PaymentIntent intent) {
        return new PaymentResponse(
                intent.getId().toString(),
                intent.getStatus().name(),
                intent.getAmount().getAmount(),
                intent.getAmount().getCurrency(),
                intent.getIdempotencyKey(),
                intent.getCreatedAt(),
                intent.getUpdatedAt()
        );
    }
}
