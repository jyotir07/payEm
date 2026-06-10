package com.paymentsplatform.ledger.internal.application

import com.paymentsplatform.ledger.internal.domain._
import com.paymentsplatform.ledger.internal.domain.exceptions.{AccountNotFoundException, InvariantViolationException}

import java.time.Instant
import java.util.UUID

/**
  * Records a balanced set of ledger entries for a single payment.
  *
  * Idempotent on paymentId: if entries already exist for the payment, returns
  * those without writing. Otherwise validates the double-entry invariant via
  * `DoubleEntryValidator` and appends atomically.
  */
final class RecordTransactionUseCase(
    accountRepository: AccountRepository,
    ledgerRepository: LedgerRepository
) {

  def execute(request: RecordTransactionUseCase.Request): RecordTransactionUseCase.Result = {
    val existing = ledgerRepository.findEntriesByPayment(request.paymentId)
    if (existing.nonEmpty)
      return RecordTransactionUseCase.Result(entries = existing, isNew = false)

    val entries = request.legs.map { leg =>
      val account = accountRepository.findById(leg.accountId)
        .getOrElse(throw AccountNotFoundException(leg.accountId))
      val amount = Money.of(leg.amount, leg.currency)
      if (amount.currency != account.currency)
        throw InvariantViolationException(
          request.paymentId,
          s"Account ${account.id} currency ${account.currency} does not match entry ${amount.currency}"
        )
      LedgerEntry(
        id = UUID.randomUUID(),
        accountId = leg.accountId,
        entryType = leg.entryType,
        amount = amount,
        paymentId = request.paymentId,
        createdAt = Instant.now()
      )
    }

    DoubleEntryValidator.validate(entries)
    ledgerRepository.appendEntries(entries)
    RecordTransactionUseCase.Result(entries = entries, isNew = true)
  }
}

object RecordTransactionUseCase {

  final case class Leg(
      accountId: UUID,
      entryType: EntryType,
      amount: BigDecimal,
      currency: String
  )

  final case class Request(paymentId: UUID, legs: List[Leg])

  final case class Result(entries: List[LedgerEntry], isNew: Boolean)
}
