package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvalidMoneyException

import java.util.Currency
import scala.util.Try

final case class Money private (amount: BigDecimal, currency: String) {

  def add(other: Money): Money = {
    requireSameCurrency(other, "add")
    Money(amount + other.amount, currency)
  }

  def subtract(other: Money): Money = {
    requireSameCurrency(other, "subtract")
    Money(amount - other.amount, currency)
  }

  def isGreaterThan(other: Money): Boolean = {
    requireSameCurrency(other, "compare")
    amount > other.amount
  }

  def isZero: Boolean = amount.compare(BigDecimal(0)) == 0

  private def requireSameCurrency(other: Money, op: String): Unit =
    if (currency != other.currency)
      throw InvalidMoneyException(
        s"Currency mismatch on $op: $currency vs ${other.currency}"
      )

  override def toString: String = s"$amount $currency"
}

object Money {

  def of(amount: BigDecimal, currency: String): Money = {
    if (amount == null) throw InvalidMoneyException("Amount must not be null")
    if (currency == null || currency.trim.isEmpty)
      throw InvalidMoneyException("Currency must not be null or empty")
    val normalized = currency.trim.toUpperCase
    if (Try(Currency.getInstance(normalized)).isFailure)
      throw InvalidMoneyException(s"Unknown ISO 4217 currency: $currency")
    if (amount.signum < 0)
      throw InvalidMoneyException(s"Amount must be non-negative: $amount")
    Money(amount, normalized)
  }

  def zero(currency: String): Money = of(BigDecimal(0), currency)
}
