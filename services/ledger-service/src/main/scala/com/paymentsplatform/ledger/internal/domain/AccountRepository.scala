package com.paymentsplatform.ledger.internal.domain

import java.util.UUID

trait AccountRepository {

  def save(account: Account): Unit

  def findById(id: UUID): Option[Account]
}
