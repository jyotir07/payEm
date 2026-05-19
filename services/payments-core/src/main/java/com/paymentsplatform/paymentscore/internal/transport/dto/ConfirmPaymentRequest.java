package com.paymentsplatform.paymentscore.internal.transport.dto;

/**
 * Inbound DTO for POST /payments/{id}/confirm.
 *
 * outcome: "succeeded" or "failed" (case-insensitive).
 */
public record ConfirmPaymentRequest(String outcome) {
}
