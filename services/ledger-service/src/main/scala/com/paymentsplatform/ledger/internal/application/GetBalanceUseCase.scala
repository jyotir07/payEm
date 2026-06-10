package com.paymentsplatform.ledger.internal.application

import com.paymentsplatform.ledger.internal.domain.exceptions.AccountNotFoundException
import com.paymentsplatform.ledger.internal.domain.{Account, AccountRepository, Money}

import java.util.UUID

final class GetBalanceUseCase(accountRepository: AccountRepository) {

  def execute(accountId: UUID): Money =
    loadAccount(accountId).balance

  def loadAccount(accountId: UUID): Account =
    accountRepository.findById(accountId).getOrElse(throw AccountNotFoundException(accountId))
}
