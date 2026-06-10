// Application entrypoint for ledger-service. Wires the dependency graph by hand:
// DataSource → repositories → use cases → controller → Javalin.

package com.paymentsplatform.ledger

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.paymentsplatform.ledger.internal.application._
import com.paymentsplatform.ledger.internal.infrastructure.{PostgresAccountRepository, PostgresLedgerRepository}
import com.paymentsplatform.ledger.internal.transport.{ErrorHandler, LedgerController}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.javalin.Javalin
import io.javalin.json.JavalinJackson

import javax.sql.DataSource

object Main {

  def main(args: Array[String]): Unit = {
    val dataSource = buildDataSource()
    val app = buildApp(dataSource)
    val port = sys.env.getOrElse("PORT", "8081").toInt
    app.start(port)
  }

  /** Builds the fully-wired Javalin app. Exposed for tests. */
  def buildApp(dataSource: DataSource): Javalin = {
    val accountRepo = new PostgresAccountRepository(dataSource)
    val ledgerRepo  = new PostgresLedgerRepository(dataSource)

    val openAccount         = new OpenAccountUseCase(accountRepo)
    val getBalance          = new GetBalanceUseCase(accountRepo)
    val recordTransaction   = new RecordTransactionUseCase(accountRepo, ledgerRepo)
    val getEntriesForPayment = new GetEntriesForPaymentUseCase(ledgerRepo)

    val controller = new LedgerController(openAccount, getBalance, recordTransaction, getEntriesForPayment)

    val app = Javalin.create { config =>
      config.jsonMapper(new JavalinJackson(buildObjectMapper(), true))
    }
    controller.register(app)
    ErrorHandler.register(app)
    app
  }

  private def buildObjectMapper(): ObjectMapper =
    new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .registerModule(DefaultScalaModule)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  private def buildDataSource(): DataSource = {
    val config = new HikariConfig()
    config.setJdbcUrl(sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://localhost:5432/ledger"))
    config.setUsername(sys.env.getOrElse("DB_USER", "ledger"))
    config.setPassword(sys.env.getOrElse("DB_PASSWORD", "ledger"))
    new HikariDataSource(config)
  }
}
