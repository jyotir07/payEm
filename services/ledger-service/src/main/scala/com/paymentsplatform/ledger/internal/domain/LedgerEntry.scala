package com.paymentsplatform.ledger.internal.domain

import java.time.Instant
import java.util.UUID

final case class LedgerEntry(
    id: UUID,
    accountId: UUID,
    entryType: EntryType,
    amount: Money,
    paymentId: UUID,
    createdAt: Instant
)

object LedgerEntry {

  def debit(accountId: UUID, amount: Money, paymentId: UUID): LedgerEntry =
    LedgerEntry(UUID.randomUUID(), accountId, EntryType.Debit, amount, paymentId, Instant.now())

  def credit(accountId: UUID, amount: Money, paymentId: UUID): LedgerEntry =
    LedgerEntry(UUID.randomUUID(), accountId, EntryType.Credit, amount, paymentId, Instant.now())
}
