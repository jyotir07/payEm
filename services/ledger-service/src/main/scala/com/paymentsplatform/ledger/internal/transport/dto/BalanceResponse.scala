package com.paymentsplatform.ledger.internal.transport.dto

import com.paymentsplatform.ledger.internal.domain.{Account, Money}

import java.util.UUID

final case class BalanceResponse(
    accountId: UUID,
    amount: BigDecimal,
    currency: String
)

object BalanceResponse {
  def from(account: Account, balance: Money): BalanceResponse =
    BalanceResponse(account.id, balance.amount, balance.currency)
}
