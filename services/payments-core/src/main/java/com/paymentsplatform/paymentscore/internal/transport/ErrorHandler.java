package com.paymentsplatform.paymentscore.internal.transport;

import com.paymentsplatform.paymentscore.internal.domain.exceptions.DuplicateIdempotencyKeyException;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidMoneyException;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.InvalidStateTransitionException;
import com.paymentsplatform.paymentscore.internal.domain.exceptions.PaymentNotFoundException;
import com.paymentsplatform.paymentscore.internal.transport.dto.ErrorResponse;
import io.javalin.Javalin;

/**
 * Maps domain and transport exceptions onto HTTP responses.
 *
 * Mapping:
 *   PaymentNotFoundException              → 404 not_found
 *   InvalidStateTransitionException       → 422 invalid_state_transition
 *   InvalidMoneyException                 → 400 invalid_money
 *   DuplicateIdempotencyKeyException      → 409 duplicate_idempotency_key
 *       (CreatePaymentUseCase returns the existing intent rather than throwing,
 *        so this handler is a defensive fallback for any future code path that
 *        does throw it.)
 *   IllegalArgumentException              → 400 invalid_request
 *   InvalidPathParameterException         → 400 invalid_path_parameter
 *   Throwable                             → 500 internal_error
 */
public final class ErrorHandler {

    private ErrorHandler() {}

    public static void register(Javalin app) {
        app.exception(PaymentNotFoundException.class, (e, ctx) ->
                ctx.status(404).json(new ErrorResponse("not_found", e.getMessage())));

        app.exception(InvalidStateTransitionException.class, (e, ctx) ->
                ctx.status(422).json(new ErrorResponse("invalid_state_transition", e.getMessage())));

        app.exception(InvalidMoneyException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse("invalid_money", e.getMessage())));

        app.exception(DuplicateIdempotencyKeyException.class, (e, ctx) ->
                ctx.status(409).json(new ErrorResponse("duplicate_idempotency_key", e.getMessage())));

        app.exception(PaymentController.InvalidPathParameterException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse("invalid_path_parameter", e.getMessage())));

        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse("invalid_request", e.getMessage())));

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).json(new ErrorResponse(
                    "internal_error",
                    "An unexpected error occurred"
            ));
        });
    }
}
