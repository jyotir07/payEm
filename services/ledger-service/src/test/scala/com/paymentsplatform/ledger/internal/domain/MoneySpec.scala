package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvalidMoneyException
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MoneySpec extends AnyFunSpec with Matchers {

  describe("Money.of") {
    it("constructs a valid Money from BigDecimal and ISO 4217 currency") {
      val m = Money.of(BigDecimal("100.50"), "INR")
      m.amount shouldBe BigDecimal("100.50")
      m.currency shouldBe "INR"
    }

    it("normalizes currency to uppercase and trims whitespace") {
      Money.of(BigDecimal("1"), "  inr ").currency shouldBe "INR"
    }

    it("rejects negative amounts") {
      a[InvalidMoneyException] should be thrownBy Money.of(BigDecimal("-1"), "INR")
    }

    it("rejects null currency") {
      a[InvalidMoneyException] should be thrownBy Money.of(BigDecimal("1"), null)
    }

    it("rejects empty currency") {
      a[InvalidMoneyException] should be thrownBy Money.of(BigDecimal("1"), "   ")
    }

    it("rejects unknown ISO 4217 currency") {
      a[InvalidMoneyException] should be thrownBy Money.of(BigDecimal("1"), "ZZZ")
    }

    it("accepts zero") {
      Money.of(BigDecimal("0"), "USD").isZero shouldBe true
    }
  }

  describe("Money.add") {
    it("adds same-currency amounts") {
      val left = Money.of(BigDecimal("10"), "INR")
      val right = Money.of(BigDecimal("5"), "INR")
      left.add(right).amount shouldBe BigDecimal("15")
    }

    it("throws on currency mismatch") {
      val left = Money.of(BigDecimal("10"), "INR")
      val right = Money.of(BigDecimal("5"), "USD")
      an[InvalidMoneyException] should be thrownBy left.add(right)
    }

    it("is immutable — original is unchanged") {
      val left = Money.of(BigDecimal("10"), "INR")
      val right = Money.of(BigDecimal("5"), "INR")
      left.add(right)
      left.amount shouldBe BigDecimal("10")
    }
  }

  describe("Money.subtract") {
    it("subtracts same-currency amounts (allows negative result)") {
      val left = Money.of(BigDecimal("3"), "INR")
      val right = Money.of(BigDecimal("5"), "INR")
      left.subtract(right).amount shouldBe BigDecimal("-2")
    }

    it("throws on currency mismatch") {
      val left = Money.of(BigDecimal("10"), "INR")
      val right = Money.of(BigDecimal("5"), "USD")
      an[InvalidMoneyException] should be thrownBy left.subtract(right)
    }
  }

  describe("Money.isGreaterThan") {
    it("compares same-currency amounts") {
      Money.of(BigDecimal("10"), "INR").isGreaterThan(Money.of(BigDecimal("5"), "INR")) shouldBe true
      Money.of(BigDecimal("3"), "INR").isGreaterThan(Money.of(BigDecimal("5"), "INR")) shouldBe false
    }

    it("throws on currency mismatch") {
      an[InvalidMoneyException] should be thrownBy
        Money.of(BigDecimal("10"), "INR").isGreaterThan(Money.of(BigDecimal("5"), "USD"))
    }
  }

  describe("Money equality") {
    it("equal when same amount and currency") {
      Money.of(BigDecimal("10"), "INR") shouldBe Money.of(BigDecimal("10"), "INR")
    }

    it("not equal when currency differs") {
      Money.of(BigDecimal("10"), "INR") should not be Money.of(BigDecimal("10"), "USD")
    }
  }
}
