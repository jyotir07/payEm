package com.paymentsplatform.ledger.internal.domain.exceptions

import java.util.UUID

final case class InvariantViolationException(
    paymentId: UUID,
    message: String
) extends RuntimeException(s"$message (paymentId=$paymentId)")
