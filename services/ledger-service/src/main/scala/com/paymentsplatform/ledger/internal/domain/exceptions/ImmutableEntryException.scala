package com.paymentsplatform.ledger.internal.domain.exceptions

final case class ImmutableEntryException(message: String)
    extends RuntimeException(message)
