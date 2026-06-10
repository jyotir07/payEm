package com.paymentsplatform.ledger.internal.infrastructure

import com.paymentsplatform.ledger.internal.domain.{Account, AccountRepository, EntryType, LedgerEntry, Money}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

final class PostgresAccountRepository(dataSource: DataSource) extends AccountRepository {

  override def save(account: Account): Unit = {
    val sql =
      """INSERT INTO accounts (id, name, currency, created_at)
        |VALUES (?, ?, ?, ?)
        |ON CONFLICT (id) DO NOTHING""".stripMargin

    withConnection { conn =>
      val ps = conn.prepareStatement(sql)
      try {
        ps.setObject(1, account.id)
        ps.setString(2, account.name)
        ps.setString(3, account.currency)
        ps.setTimestamp(4, Timestamp.from(Instant.now()))
        ps.executeUpdate()
      } finally ps.close()
    }
  }

  override def findById(id: UUID): Option[Account] = {
    val sql = "SELECT id, name, currency FROM accounts WHERE id = ?"
    withConnection { conn =>
      val ps = conn.prepareStatement(sql)
      try {
        ps.setObject(1, id)
        val rs = ps.executeQuery()
        try {
          if (rs.next()) {
            val name = rs.getString("name")
            val currency = rs.getString("currency")
            val entries = loadEntriesForAccount(conn, id)
            Some(Account(id, name, currency, entries))
          } else None
        } finally rs.close()
      } finally ps.close()
    }
  }

  private def loadEntriesForAccount(conn: Connection, accountId: UUID): List[LedgerEntry] = {
    val sql =
      """SELECT id, account_id, entry_type, amount_value, amount_currency, payment_id, created_at
        |FROM ledger_entries
        |WHERE account_id = ?
        |ORDER BY created_at ASC, id ASC""".stripMargin
    val ps = conn.prepareStatement(sql)
    try {
      ps.setObject(1, accountId)
      val rs = ps.executeQuery()
      try mapRows(rs) finally rs.close()
    } finally ps.close()
  }

  private def mapRows(rs: ResultSet): List[LedgerEntry] = {
    val buf = scala.collection.mutable.ListBuffer.empty[LedgerEntry]
    while (rs.next()) {
      buf += LedgerEntry(
        id = rs.getObject("id", classOf[UUID]),
        accountId = rs.getObject("account_id", classOf[UUID]),
        entryType = EntryType.fromName(rs.getString("entry_type")),
        amount = Money.of(BigDecimal(rs.getBigDecimal("amount_value")), rs.getString("amount_currency")),
        paymentId = rs.getObject("payment_id", classOf[UUID]),
        createdAt = rs.getTimestamp("created_at").toInstant
      )
    }
    buf.toList
  }

  private def withConnection[T](f: Connection => T): T = {
    val conn = dataSource.getConnection
    try f(conn) finally conn.close()
  }
}
