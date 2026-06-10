package com.paymentsplatform.ledger.internal.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.paymentsplatform.ledger.Main
import com.paymentsplatform.ledger.internal.application.OpenAccountUseCase
import com.rabbitmq.client.{Connection, ConnectionFactory, MessageProperties}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.testcontainers.containers.{PostgreSQLContainer, RabbitMQContainer}

import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.sql.DataSource
import scala.util.Using

/**
  * End-to-end spec for the Phase 6 RabbitMQ consumer.
  *
  * Stands up Postgres + RabbitMQ via Testcontainers, boots the consumer, publishes
  * a PaymentSucceeded JSON envelope, and asserts that a balanced debit/credit pair
  * lands in the ledger for the configured cash + revenue accounts.
  */
class RabbitMqPaymentEventConsumerSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll with Eventually {

  private val postgres = new PostgreSQLContainer("postgres:16")
    .withDatabaseName("ledger_test").withUsername("test").withPassword("test")
  private val rabbit = new RabbitMQContainer("rabbitmq:3.13-management")

  private var dataSource: DataSource = _
  private var rabbitConnection: Connection = _
  private var consumer: RabbitMqPaymentEventConsumer = _
  private var publishChannel: com.rabbitmq.client.Channel = _

  private var cashAccountId: UUID = _
  private var revenueAccountId: UUID = _
  private var recordTransaction: com.paymentsplatform.ledger.internal.application.RecordTransactionUseCase = _
  private var entriesByPayment: com.paymentsplatform.ledger.internal.application.GetEntriesForPaymentUseCase = _

  private val mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .registerModule(DefaultScalaModule)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(1, Seconds))

  override def beforeAll(): Unit = {
    postgres.start()
    rabbit.start()

    val hc = new HikariConfig()
    hc.setJdbcUrl(postgres.getJdbcUrl)
    hc.setUsername(postgres.getUsername)
    hc.setPassword(postgres.getPassword)
    dataSource = new HikariDataSource(hc)
    applyMigrations()

    val (_, openAccount, _, record, getEntries) = Main.buildUseCases(dataSource)
    recordTransaction = record
    entriesByPayment  = getEntries

    cashAccountId    = openAccount.execute("Cash", "INR").id
    revenueAccountId = openAccount.execute("Revenue", "INR").id

    val factory = new ConnectionFactory()
    factory.setHost(rabbit.getHost)
    factory.setPort(rabbit.getAmqpPort)
    factory.setUsername(rabbit.getAdminUsername)
    factory.setPassword(rabbit.getAdminPassword)
    rabbitConnection = factory.newConnection()

    consumer = new RabbitMqPaymentEventConsumer(
      rabbitConnection,
      mapper,
      recordTransaction,
      cashAccountId,
      revenueAccountId
    )
    consumer.start()

    publishChannel = rabbitConnection.createChannel()
    publishChannel.exchangeDeclare(RabbitMqPaymentEventConsumer.Exchange, "topic", true)
  }

  override def afterAll(): Unit = {
    try if (publishChannel != null) publishChannel.close() catch { case _: Throwable => }
    try if (consumer != null) consumer.close()                 catch { case _: Throwable => }
    try if (rabbitConnection != null) rabbitConnection.close() catch { case _: Throwable => }
    try if (rabbit != null) rabbit.stop()                      catch { case _: Throwable => }
    try if (postgres != null) postgres.stop()                  catch { case _: Throwable => }
  }

  describe("RabbitMqPaymentEventConsumer") {

    it("records a balanced debit/credit pair when PaymentSucceeded arrives") {
      val paymentId = UUID.randomUUID()
      publish(paymentSucceededJson(paymentId, "1000.00", "INR"))

      eventually {
        val entries = entriesByPayment.execute(paymentId)
        entries.size shouldBe 2
        entries.exists(e => e.accountId == cashAccountId && e.entryType.name == "DEBIT") shouldBe true
        entries.exists(e => e.accountId == revenueAccountId && e.entryType.name == "CREDIT") shouldBe true
        entries.forall(_.amount.amount == BigDecimal("1000.0000")) shouldBe true
      }
    }

    it("is idempotent on paymentId — duplicate delivery does not double the entries") {
      val paymentId = UUID.randomUUID()
      val payload = paymentSucceededJson(paymentId, "250.00", "INR")
      publish(payload)
      publish(payload)
      publish(payload)

      // The consumer's RecordTransactionUseCase short-circuits on findEntriesByPayment,
      // so even if every delivery is processed the ledger still has exactly two entries.
      eventually {
        entriesByPayment.execute(paymentId).size shouldBe 2
      }
    }

    it("acks and discards a malformed payload without poisoning the queue") {
      val poisonPaymentId = UUID.randomUUID()
      publish("{ not valid json")
      val goodId = UUID.randomUUID()
      publish(paymentSucceededJson(goodId, "10.00", "INR"))

      eventually {
        entriesByPayment.execute(goodId).size shouldBe 2
        entriesByPayment.execute(poisonPaymentId).size shouldBe 0
      }
    }
  }

  // ---- helpers ----

  private def applyMigrations(): Unit = {
    val stream = getClass.getClassLoader.getResourceAsStream("V1__create_ledger_tables.sql")
    require(stream != null, "Migration not on classpath")
    val sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    Using.resource(dataSource.getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        stmt.execute(sql)
      }
    }
  }

  private def publish(json: String): Unit = {
    publishChannel.basicPublish(
      RabbitMqPaymentEventConsumer.Exchange,
      RabbitMqPaymentEventConsumer.RoutingKey,
      MessageProperties.PERSISTENT_TEXT_PLAIN,
      json.getBytes(StandardCharsets.UTF_8)
    )
  }

  private def paymentSucceededJson(paymentId: UUID, amount: String, currency: String): String =
    s"""{
       |  "eventType": "PaymentSucceeded",
       |  "schemaVersion": 1,
       |  "paymentId": "$paymentId",
       |  "amount": "$amount",
       |  "currency": "$currency",
       |  "timestamp": "2026-06-10T12:34:56Z"
       |}""".stripMargin
}
