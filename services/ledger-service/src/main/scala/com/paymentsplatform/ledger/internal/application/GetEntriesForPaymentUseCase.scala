package com.paymentsplatform.ledger.internal.application

import com.paymentsplatform.ledger.internal.domain.{LedgerEntry, LedgerRepository}

import java.util.UUID

final class GetEntriesForPaymentUseCase(ledgerRepository: LedgerRepository) {

  def execute(paymentId: UUID): List[LedgerEntry] =
    ledgerRepository.findEntriesByPayment(paymentId)
}
