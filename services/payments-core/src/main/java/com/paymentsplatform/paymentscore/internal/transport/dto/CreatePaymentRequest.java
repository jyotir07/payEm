package com.paymentsplatform.paymentscore.internal.transport.dto;

import java.math.BigDecimal;

/**
 * Inbound DTO for POST /payments. Decoupled from the domain so wire format can evolve
 * without touching domain types.
 */
public record CreatePaymentRequest(BigDecimal amount, String currency) {
}
