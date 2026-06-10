package com.paymentsplatform.ledger.internal.application

import com.paymentsplatform.ledger.internal.domain.{Account, AccountRepository}

final class OpenAccountUseCase(accountRepository: AccountRepository) {

  def execute(name: String, currency: String): Account = {
    val account = Account.open(name, currency)
    accountRepository.save(account)
    account
  }
}
