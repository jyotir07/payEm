package com.paymentsplatform.ledger.internal.domain

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class LedgerEntrySpec extends AnyFunSpec with Matchers {

  describe("LedgerEntry.debit") {
    it("creates a DEBIT entry with the given account, amount, and payment") {
      val accountId = UUID.randomUUID()
      val paymentId = UUID.randomUUID()
      val amount = Money.of(BigDecimal("100"), "INR")
      val e = LedgerEntry.debit(accountId, amount, paymentId)
      e.accountId shouldBe accountId
      e.paymentId shouldBe paymentId
      e.amount shouldBe amount
      e.entryType shouldBe EntryType.Debit
      e.id should not be null
      e.createdAt should not be null
    }
  }

  describe("LedgerEntry.credit") {
    it("creates a CREDIT entry with the given account, amount, and payment") {
      val accountId = UUID.randomUUID()
      val paymentId = UUID.randomUUID()
      val amount = Money.of(BigDecimal("100"), "INR")
      val e = LedgerEntry.credit(accountId, amount, paymentId)
      e.entryType shouldBe EntryType.Credit
    }
  }

  describe("LedgerEntry immutability") {
    it("is a case class with no mutators") {
      val e = LedgerEntry.debit(UUID.randomUUID(), Money.of(BigDecimal("1"), "INR"), UUID.randomUUID())
      val copy = e.copy(amount = Money.of(BigDecimal("999"), "INR"))
      copy.amount.amount shouldBe BigDecimal("999")
      e.amount.amount shouldBe BigDecimal("1")
    }
  }
}
