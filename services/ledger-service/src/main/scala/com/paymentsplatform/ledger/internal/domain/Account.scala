package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvalidMoneyException

import java.util.UUID

final case class Account(
    id: UUID,
    name: String,
    currency: String,
    entries: List[LedgerEntry]
) {

  def applyEntry(entry: LedgerEntry): Account = {
    if (entry.accountId != id)
      throw InvalidMoneyException(
        s"Entry accountId ${entry.accountId} does not belong to account $id"
      )
    if (entry.amount.currency != currency)
      throw InvalidMoneyException(
        s"Entry currency ${entry.amount.currency} does not match account currency $currency"
      )
    copy(entries = entries :+ entry)
  }

  def balance: Money = {
    val zero = Money.zero(currency)
    entries.foldLeft(zero) { (acc, e) =>
      e.entryType match {
        case EntryType.Credit => acc.add(e.amount)
        case EntryType.Debit  => acc.subtract(e.amount)
      }
    }
  }
}

object Account {

  def open(name: String, currency: String): Account =
    Account(UUID.randomUUID(), name, Money.of(BigDecimal(0), currency).currency, Nil)
}
