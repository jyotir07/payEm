package com.paymentsplatform.paymentscore.internal.transport.dto;

/**
 * Uniform error envelope returned by the API.
 *
 * code  — stable machine-readable identifier (e.g. "invalid_state_transition")
 * message — human-readable detail
 */
public record ErrorResponse(String code, String message) {
}
