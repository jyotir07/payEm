package com.paymentsplatform.ledger.internal.domain

import com.paymentsplatform.ledger.internal.domain.exceptions.InvariantViolationException
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class DoubleEntryValidatorSpec extends AnyFunSpec with Matchers {

  private def acct() = UUID.randomUUID()

  describe("DoubleEntryValidator.validate") {
    it("accepts a balanced single-debit single-credit pair") {
      val paymentId = UUID.randomUUID()
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("100"), "INR"), paymentId),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("100"), "INR"), paymentId)
      )
      noException should be thrownBy DoubleEntryValidator.validate(entries)
      DoubleEntryValidator.isValid(entries) shouldBe true
    }

    it("accepts a multi-leg transaction whose debits and credits sum to equal totals") {
      val paymentId = UUID.randomUUID()
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("70"), "INR"), paymentId),
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("30"), "INR"), paymentId),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("100"), "INR"), paymentId)
      )
      noException should be thrownBy DoubleEntryValidator.validate(entries)
    }

    it("rejects an empty entry list") {
      an[InvariantViolationException] should be thrownBy DoubleEntryValidator.validate(Nil)
    }

    it("rejects when debits != credits") {
      val paymentId = UUID.randomUUID()
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("100"), "INR"), paymentId),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("90"), "INR"), paymentId)
      )
      an[InvariantViolationException] should be thrownBy DoubleEntryValidator.validate(entries)
    }

    it("rejects entries spanning multiple paymentIds") {
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("100"), "INR"), UUID.randomUUID()),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("100"), "INR"), UUID.randomUUID())
      )
      an[InvariantViolationException] should be thrownBy DoubleEntryValidator.validate(entries)
    }

    it("rejects entries in different currencies") {
      val paymentId = UUID.randomUUID()
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("100"), "INR"), paymentId),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("100"), "USD"), paymentId)
      )
      an[InvariantViolationException] should be thrownBy DoubleEntryValidator.validate(entries)
    }

    it("rejects a zero-value transaction") {
      val paymentId = UUID.randomUUID()
      val entries = List(
        LedgerEntry.debit(acct(),  Money.of(BigDecimal("0"), "INR"), paymentId),
        LedgerEntry.credit(acct(), Money.of(BigDecimal("0"), "INR"), paymentId)
      )
      an[InvariantViolationException] should be thrownBy DoubleEntryValidator.validate(entries)
    }

    it("isValid returns false instead of throwing") {
      DoubleEntryValidator.isValid(Nil) shouldBe false
    }
  }
}
