// Application entrypoint for ledger-service. Wires the dependency graph by hand:
// DataSource → repositories → use cases → controller → Javalin → optional RabbitMQ consumer.

package com.paymentsplatform.ledger

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.paymentsplatform.ledger.internal.application._
import com.paymentsplatform.ledger.internal.infrastructure.{
  PostgresAccountRepository,
  PostgresLedgerRepository,
  RabbitMqPaymentEventConsumer
}
import com.paymentsplatform.ledger.internal.transport.{ErrorHandler, LedgerController}
import com.rabbitmq.client.{Connection, ConnectionFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.javalin.Javalin
import io.javalin.json.JavalinJackson

import java.util.UUID
import javax.sql.DataSource

object Main {

  def main(args: Array[String]): Unit = {
    val dataSource = buildDataSource()
    val app = buildApp(dataSource)
    val port = sys.env.getOrElse("PORT", "8081").toInt
    app.start(port)

    startEventConsumerIfConfigured(dataSource)
  }

  /** Builds the fully-wired Javalin app. Exposed for tests. */
  def buildApp(dataSource: DataSource): Javalin = {
    val (controller, _, _, _, _) = buildUseCases(dataSource)
    val app = Javalin.create { config =>
      config.jsonMapper(new JavalinJackson(buildObjectMapper(), true))
    }
    controller.register(app)
    ErrorHandler.register(app)
    app
  }

  /**
    * Builds the use-case stack and returns the wired controller along with the
    * pieces a RabbitMQ consumer needs.  Exposed for tests that wire the consumer
    * directly without standing up Javalin.
    */
  def buildUseCases(dataSource: DataSource): (
      LedgerController,
      OpenAccountUseCase,
      GetBalanceUseCase,
      RecordTransactionUseCase,
      GetEntriesForPaymentUseCase
  ) = {
    val accountRepo = new PostgresAccountRepository(dataSource)
    val ledgerRepo  = new PostgresLedgerRepository(dataSource)

    val openAccount         = new OpenAccountUseCase(accountRepo)
    val getBalance          = new GetBalanceUseCase(accountRepo)
    val recordTransaction   = new RecordTransactionUseCase(accountRepo, ledgerRepo)
    val getEntriesForPayment = new GetEntriesForPaymentUseCase(ledgerRepo)

    val controller = new LedgerController(openAccount, getBalance, recordTransaction, getEntriesForPayment)
    (controller, openAccount, getBalance, recordTransaction, getEntriesForPayment)
  }

  /**
    * Optionally boots a RabbitMQ consumer on PaymentSucceeded events. Requires
    * RABBITMQ_URL, LEDGER_CASH_ACCOUNT_ID, and LEDGER_REVENUE_ACCOUNT_ID env vars
    * to be set; otherwise logs a notice and skips so the service still boots
    * in environments without a broker.
    */
  private def startEventConsumerIfConfigured(dataSource: DataSource): Unit = {
    val url      = sys.env.getOrElse("RABBITMQ_URL", "")
    val cashEnv  = sys.env.getOrElse("LEDGER_CASH_ACCOUNT_ID", "")
    val revEnv   = sys.env.getOrElse("LEDGER_REVENUE_ACCOUNT_ID", "")

    if (url.isEmpty || cashEnv.isEmpty || revEnv.isEmpty) {
      println("[ledger-service] RABBITMQ_URL / LEDGER_CASH_ACCOUNT_ID / LEDGER_REVENUE_ACCOUNT_ID not all set — payment event consumer disabled")
      return
    }
    try {
      val factory = new ConnectionFactory()
      factory.setUri(url)
      val connection: Connection = factory.newConnection()
      val (_, _, _, recordTransaction, _) = buildUseCases(dataSource)
      val consumer = new RabbitMqPaymentEventConsumer(
        connection,
        buildObjectMapper(),
        recordTransaction,
        UUID.fromString(cashEnv),
        UUID.fromString(revEnv)
      )
      consumer.start()
      Runtime.getRuntime.addShutdownHook(new Thread(() => {
        consumer.close()
        connection.close()
      }))
      println("[ledger-service] payment event consumer started")
    } catch {
      case t: Throwable =>
        System.err.println(s"[ledger-service] failed to start payment event consumer: ${t.getMessage}")
    }
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
