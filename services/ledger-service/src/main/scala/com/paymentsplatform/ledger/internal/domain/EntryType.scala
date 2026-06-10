package com.paymentsplatform.ledger.internal.domain

sealed trait EntryType {
  def name: String
}

object EntryType {
  case object Debit extends EntryType { val name = "DEBIT" }
  case object Credit extends EntryType { val name = "CREDIT" }

  def fromName(name: String): EntryType = name.toUpperCase match {
    case "DEBIT"  => Debit
    case "CREDIT" => Credit
    case other    => throw new IllegalArgumentException(s"Unknown EntryType: $other")
  }
}
