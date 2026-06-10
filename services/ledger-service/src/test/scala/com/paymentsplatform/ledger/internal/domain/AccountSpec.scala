package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvalidMoneyException
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class AccountSpec extends AnyFunSpec with Matchers {

  describe("Account.open") {
    it("creates an empty account with the given name and currency") {
      val a = Account.open("Cash", "INR")
      a.name shouldBe "Cash"
      a.currency shouldBe "INR"
      a.entries shouldBe empty
      a.balance shouldBe Money.zero("INR")
    }
  }

  describe("Account.applyEntry") {
    it("returns a new account with the entry appended (immutable)") {
      val a = Account.open("Cash", "INR")
      val e = LedgerEntry.credit(a.id, Money.of(BigDecimal("100"), "INR"), UUID.randomUUID())
      val a2 = a.applyEntry(e)
      a2.entries should have size 1
      a.entries shouldBe empty
    }

    it("rejects an entry that belongs to a different account") {
      val a = Account.open("Cash", "INR")
      val e = LedgerEntry.credit(UUID.randomUUID(), Money.of(BigDecimal("100"), "INR"), UUID.randomUUID())
      an[InvalidMoneyException] should be thrownBy a.applyEntry(e)
    }

    it("rejects an entry with mismatched currency") {
      val a = Account.open("Cash", "INR")
      val e = LedgerEntry.credit(a.id, Money.of(BigDecimal("100"), "USD"), UUID.randomUUID())
      an[InvalidMoneyException] should be thrownBy a.applyEntry(e)
    }
  }

  describe("Account.balance") {
    it("is derived: credits add and debits subtract") {
      val a = Account.open("Cash", "INR")
      val pid = UUID.randomUUID()
      val withCredit = a.applyEntry(LedgerEntry.credit(a.id, Money.of(BigDecimal("100"), "INR"), pid))
      val withDebit  = withCredit.applyEntry(LedgerEntry.debit(a.id, Money.of(BigDecimal("30"), "INR"), pid))
      withDebit.balance shouldBe Money.of(BigDecimal("70"), "INR")
    }

    it("can go negative (debit-heavy account)") {
      val a = Account.open("Liability", "INR")
      val pid = UUID.randomUUID()
      val res = a.applyEntry(LedgerEntry.debit(a.id, Money.of(BigDecimal("50"), "INR"), pid))
      res.balance.amount shouldBe BigDecimal("-50")
    }

    it("on an empty account is zero") {
      Account.open("Cash", "INR").balance shouldBe Money.zero("INR")
    }
  }
}
