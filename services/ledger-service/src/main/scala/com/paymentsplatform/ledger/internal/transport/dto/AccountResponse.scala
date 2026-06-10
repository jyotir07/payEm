package com.paymentsplatform.ledger.internal.transport.dto

import com.paymentsplatform.ledger.internal.domain.Account

import java.util.UUID

final case class AccountResponse(
    id: UUID,
    name: String,
    currency: String
)

object AccountResponse {
  def from(account: Account): AccountResponse =
    AccountResponse(account.id, account.name, account.currency)
}
