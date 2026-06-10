package com.paymentsplatform.ledger.internal.transport

import com.paymentsplatform.ledger.internal.domain.exceptions.{AccountNotFoundException, InvalidMoneyException, InvariantViolationException}
import com.paymentsplatform.ledger.internal.transport.dto.ErrorResponse
import io.javalin.Javalin

object ErrorHandler {

  def register(app: Javalin): Unit = {
    app.exception(classOf[AccountNotFoundException], (e, ctx) => {
      ctx.status(404).json(ErrorResponse("not_found", e.getMessage))
    })

    app.exception(classOf[InvariantViolationException], (e, ctx) => {
      ctx.status(422).json(ErrorResponse("invariant_violation", e.getMessage))
    })

    app.exception(classOf[InvalidMoneyException], (e, ctx) => {
      ctx.status(400).json(ErrorResponse("invalid_money", e.getMessage))
    })

    app.exception(classOf[InvalidPathParameterException], (e, ctx) => {
      ctx.status(400).json(ErrorResponse("invalid_path_parameter", e.getMessage))
    })

    app.exception(classOf[InvalidEntryTypeException], (e, ctx) => {
      ctx.status(400).json(ErrorResponse("invalid_entry_type", e.getMessage))
    })

    app.exception(classOf[IllegalArgumentException], (e, ctx) => {
      ctx.status(400).json(ErrorResponse("invalid_request", e.getMessage))
    })

    app.exception(classOf[Exception], (e, ctx) => {
      ctx.status(500).json(ErrorResponse("internal_error", Option(e.getMessage).getOrElse("Internal server error")))
    })
  }
}
