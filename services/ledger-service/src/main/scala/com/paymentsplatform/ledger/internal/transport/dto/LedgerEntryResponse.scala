package com.paymentsplatform.ledger.internal.transport.dto

import com.paymentsplatform.ledger.internal.domain.LedgerEntry

import java.time.Instant
import java.util.UUID

final case class LedgerEntryResponse(
    id: UUID,
    accountId: UUID,
    entryType: String,
    amount: BigDecimal,
    currency: String,
    paymentId: UUID,
    createdAt: Instant
)

object LedgerEntryResponse {
  def from(e: LedgerEntry): LedgerEntryResponse =
    LedgerEntryResponse(
      id = e.id,
      accountId = e.accountId,
      entryType = e.entryType.name,
      amount = e.amount.amount,
      currency = e.amount.currency,
      paymentId = e.paymentId,
      createdAt = e.createdAt
    )
}
