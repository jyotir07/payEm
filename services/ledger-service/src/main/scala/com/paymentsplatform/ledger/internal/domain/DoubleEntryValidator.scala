package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvariantViolationException

import java.util.UUID

object DoubleEntryValidator {

  def validate(entries: List[LedgerEntry]): Unit = {
    if (entries.isEmpty)
      throw InvariantViolationException(
        new UUID(0L, 0L),
        "Transaction must contain at least one debit and one credit"
      )

    val byPayment = entries.groupBy(_.paymentId)
    if (byPayment.size != 1)
      throw InvariantViolationException(
        new UUID(0L, 0L),
        s"All entries in a transaction must share the same paymentId (got ${byPayment.size})"
      )

    val paymentId = entries.head.paymentId
    val currencies = entries.map(_.amount.currency).distinct
    if (currencies.size != 1)
      throw InvariantViolationException(
        paymentId,
        s"All entries in a transaction must share the same currency (got ${currencies.mkString(", ")})"
      )

    val debits = entries.filter(_.entryType == EntryType.Debit).map(_.amount.amount).sum
    val credits = entries.filter(_.entryType == EntryType.Credit).map(_.amount.amount).sum

    if (debits.compare(credits) != 0)
      throw InvariantViolationException(
        paymentId,
        s"Double-entry violation: debits=$debits credits=$credits"
      )

    if (debits.signum <= 0)
      throw InvariantViolationException(
        paymentId,
        "Transaction must move a positive amount"
      )
  }

  def isValid(entries: List[LedgerEntry]): Boolean =
    scala.util.Try(validate(entries)).isSuccess
}
