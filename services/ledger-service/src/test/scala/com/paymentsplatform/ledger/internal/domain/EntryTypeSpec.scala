package com.paymentsplatform.ledger.internal.domain

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EntryTypeSpec extends AnyFunSpec with Matchers {

  describe("EntryType") {
    it("exposes a stable name for Debit") {
      EntryType.Debit.name shouldBe "DEBIT"
    }

    it("exposes a stable name for Credit") {
      EntryType.Credit.name shouldBe "CREDIT"
    }

    it("parses 'DEBIT' (case-insensitive)") {
      EntryType.fromName("debit") shouldBe EntryType.Debit
      EntryType.fromName("DEBIT") shouldBe EntryType.Debit
    }

    it("parses 'CREDIT' (case-insensitive)") {
      EntryType.fromName("credit") shouldBe EntryType.Credit
      EntryType.fromName("CREDIT") shouldBe EntryType.Credit
    }

    it("rejects unknown names") {
      an[IllegalArgumentException] should be thrownBy EntryType.fromName("DR")
    }
  }
}
