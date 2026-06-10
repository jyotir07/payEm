package com.paymentsplatform.ledger.internal.infrastructure

import com.paymentsplatform.ledger.internal.domain.{EntryType, LedgerEntry, LedgerRepository, Money}

import java.sql.{Connection, ResultSet, Timestamp}
import java.util.UUID
import javax.sql.DataSource

final class PostgresLedgerRepository(dataSource: DataSource) extends LedgerRepository {

  override def appendEntries(entries: List[LedgerEntry]): Unit = {
    if (entries.isEmpty) return
    val sql =
      """INSERT INTO ledger_entries
        |  (id, account_id, entry_type, amount_value, amount_currency, payment_id, created_at)
        |VALUES (?, ?, ?, ?, ?, ?, ?)""".stripMargin

    val conn = dataSource.getConnection
    try {
      conn.setAutoCommit(false)
      val ps = conn.prepareStatement(sql)
      try {
        entries.foreach { e =>
          ps.setObject(1, e.id)
          ps.setObject(2, e.accountId)
          ps.setString(3, e.entryType.name)
          ps.setBigDecimal(4, e.amount.amount.bigDecimal)
          ps.setString(5, e.amount.currency)
          ps.setObject(6, e.paymentId)
          ps.setTimestamp(7, Timestamp.from(e.createdAt))
          ps.addBatch()
        }
        ps.executeBatch()
        conn.commit()
      } catch {
        case t: Throwable =>
          conn.rollback()
          throw t
      } finally ps.close()
    } finally conn.close()
  }

  override def findEntriesByAccount(accountId: UUID): List[LedgerEntry] =
    queryEntries("WHERE account_id = ?", _.setObject(1, accountId))

  override def findEntriesByPayment(paymentId: UUID): List[LedgerEntry] =
    queryEntries("WHERE payment_id = ?", _.setObject(1, paymentId))

  private def queryEntries(whereClause: String, bind: java.sql.PreparedStatement => Unit): List[LedgerEntry] = {
    val sql =
      s"""SELECT id, account_id, entry_type, amount_value, amount_currency, payment_id, created_at
         |FROM ledger_entries
         |$whereClause
         |ORDER BY created_at ASC, id ASC""".stripMargin

    val conn = dataSource.getConnection
    try {
      val ps = conn.prepareStatement(sql)
      try {
        bind(ps)
        val rs = ps.executeQuery()
        try mapRows(rs) finally rs.close()
      } finally ps.close()
    } finally conn.close()
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
}
