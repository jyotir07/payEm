package com.paymentsplatform.ledger.internal.domain.exceptions

final case class InvalidMoneyException(message: String)
    extends RuntimeException(message)
