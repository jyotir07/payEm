package com.paymentsplatform.ledger.internal.domain

import java.util.UUID

trait LedgerRepository {

  /**
    * Append a balanced set of entries for a single transaction. Inserts are
    * atomic — either all entries land or none do. No update path exists.
    */
  def appendEntries(entries: List[LedgerEntry]): Unit

  def findEntriesByAccount(accountId: UUID): List[LedgerEntry]

  def findEntriesByPayment(paymentId: UUID): List[LedgerEntry]
}
