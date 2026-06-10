package com.paymentsplatform.ledger.internal.transport.dto

import com.paymentsplatform.ledger.internal.domain.LedgerEntry

import java.util.UUID

final case class TransactionResponse(
    paymentId: UUID,
    entries: List[LedgerEntryResponse]
)

object TransactionResponse {
  def from(paymentId: UUID, entries: List[LedgerEntry]): TransactionResponse =
    TransactionResponse(paymentId, entries.map(LedgerEntryResponse.from))
}
