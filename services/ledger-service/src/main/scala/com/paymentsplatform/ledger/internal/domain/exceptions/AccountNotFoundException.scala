package com.paymentsplatform.ledger.internal.domain.exceptions

import java.util.UUID

final case class AccountNotFoundException(accountId: UUID)
    extends RuntimeException(s"Account not found: $accountId")
