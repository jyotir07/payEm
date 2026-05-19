package com.paymentsplatform.paymentscore.internal.transport;

import com.paymentsplatform.paymentscore.internal.application.CancelPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.ConfirmPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.CreatePaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.GetPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.ProcessPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.transport.dto.ConfirmPaymentRequest;
import com.paymentsplatform.paymentscore.internal.transport.dto.CreatePaymentRequest;
import com.paymentsplatform.paymentscore.internal.transport.dto.ErrorResponse;
import com.paymentsplatform.paymentscore.internal.transport.dto.PaymentResponse;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.UUID;

/**
 * HTTP routes for payment operations. Contains zero business logic — each handler
 * validates input, calls a use case, and maps the result onto an HTTP response.
 */
public class PaymentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            ProcessPaymentUseCase processPaymentUseCase,
            ConfirmPaymentUseCase confirmPaymentUseCase,
            CancelPaymentUseCase cancelPaymentUseCase) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.processPaymentUseCase = processPaymentUseCase;
        this.confirmPaymentUseCase = confirmPaymentUseCase;
        this.cancelPaymentUseCase = cancelPaymentUseCase;
    }

    public void register(Javalin app) {
        app.post("/payments", this::create);
        app.get("/payments/{id}", this::get);
        app.post("/payments/{id}/process", this::process);
        app.post("/payments/{id}/confirm", this::confirm);
        app.post("/payments/{id}/cancel", this::cancel);
    }

    private void create(Context ctx) {
        String idempotencyKey = ctx.header(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            ctx.status(400).json(new ErrorResponse(
                    "missing_idempotency_key",
                    "Idempotency-Key header is required"
            ));
            return;
        }

        CreatePaymentRequest req = ctx.bodyAsClass(CreatePaymentRequest.class);
        if (req == null || req.amount() == null || req.currency() == null) {
            ctx.status(400).json(new ErrorResponse(
                    "invalid_request",
                    "Fields 'amount' and 'currency' are required"
            ));
            return;
        }

        CreatePaymentUseCase.Result result = createPaymentUseCase.execute(
                req.amount(), req.currency(), idempotencyKey
        );
        ctx.status(result.isNew() ? 201 : 200)
           .json(PaymentResponse.from(result.getPaymentIntent()));
    }

    private void get(Context ctx) {
        UUID id = parseUuid(ctx);
        PaymentIntent intent = getPaymentUseCase.execute(id);
        ctx.status(200).json(PaymentResponse.from(intent));
    }

    private void process(Context ctx) {
        UUID id = parseUuid(ctx);
        PaymentIntent intent = processPaymentUseCase.execute(id);
        ctx.status(200).json(PaymentResponse.from(intent));
    }

    private void confirm(Context ctx) {
        UUID id = parseUuid(ctx);
        ConfirmPaymentRequest req = ctx.bodyAsClass(ConfirmPaymentRequest.class);
        if (req == null || req.outcome() == null || req.outcome().isBlank()) {
            ctx.status(400).json(new ErrorResponse(
                    "invalid_request",
                    "Field 'outcome' is required ('succeeded' or 'failed')"
            ));
            return;
        }
        ConfirmPaymentUseCase.Outcome outcome;
        try {
            outcome = ConfirmPaymentUseCase.Outcome.valueOf(req.outcome().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse(
                    "invalid_outcome",
                    "Outcome must be 'succeeded' or 'failed', got: " + req.outcome()
            ));
            return;
        }
        PaymentIntent intent = confirmPaymentUseCase.execute(id, outcome);
        ctx.status(200).json(PaymentResponse.from(intent));
    }

    private void cancel(Context ctx) {
        UUID id = parseUuid(ctx);
        PaymentIntent intent = cancelPaymentUseCase.execute(id);
        ctx.status(200).json(PaymentResponse.from(intent));
    }

    private UUID parseUuid(Context ctx) {
        try {
            return UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            throw new InvalidPathParameterException("id must be a valid UUID: " + ctx.pathParam("id"));
        }
    }

    /**
     * Signals that a path parameter could not be parsed. Mapped to 400 by ErrorHandler.
     */
    public static class InvalidPathParameterException extends RuntimeException {
        public InvalidPathParameterException(String message) {
            super(message);
        }
    }
}
