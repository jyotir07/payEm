package com.paymentsplatform.ledger.internal.transport

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.paymentsplatform.ledger.Main
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.javalin.Javalin
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.PostgreSQLContainer

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource
import scala.util.Using

/**
  * End-to-end HTTP test of the Phase 5 transport layer against a real Postgres.
  *
  * Boots the full app via Main.buildApp(), starts Javalin on a random port,
  * and exercises every route with the JDK HttpClient.
  */
class LedgerControllerIntegrationSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll {

  private val postgres = new PostgreSQLContainer("postgres:16")
    .withDatabaseName("ledger_test")
    .withUsername("test")
    .withPassword("test")

  private var dataSource: DataSource = _
  private var app: Javalin = _
  private var port: Int = _

  private val client = HttpClient.newHttpClient()
  private val mapper = new ObjectMapper().registerModule(new JavaTimeModule())

  override def beforeAll(): Unit = {
    postgres.start()
    val hc = new HikariConfig()
    hc.setJdbcUrl(postgres.getJdbcUrl)
    hc.setUsername(postgres.getUsername)
    hc.setPassword(postgres.getPassword)
    dataSource = new HikariDataSource(hc)
    applyMigrations()

    app = Main.buildApp(dataSource)
    app.start(0)
    port = app.port()
  }

  override def afterAll(): Unit = {
    if (app != null) app.stop()
    postgres.stop()
  }

  private def applyMigrations(): Unit = {
    val stream = getClass.getClassLoader.getResourceAsStream("V1__create_ledger_tables.sql")
    require(stream != null, "Migration not on classpath")
    val sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    Using.resource(dataSource.getConnection) { conn: Connection =>
      Using.resource(conn.createStatement()) { stmt =>
        stmt.execute(sql)
      }
    }
  }

  // ---- happy path ----

  describe("Ledger HTTP") {

    it("creates an account, records a balanced transaction, and returns the derived balance") {
      val cashId = createAccount("Cash", "INR")
      val revenueId = createAccount("Revenue", "INR")
      val paymentId = UUID.randomUUID()

      val body =
        s"""{
           |  "paymentId": "$paymentId",
           |  "legs": [
           |    { "accountId": "$cashId",    "entryType": "DEBIT",  "amount": 100.00, "currency": "INR" },
           |    { "accountId": "$revenueId", "entryType": "CREDIT", "amount": 100.00, "currency": "INR" }
           |  ]
           |}""".stripMargin

      val recorded = post("/entries", body)
      recorded.statusCode() shouldBe 201
      val recordedJson = mapper.readTree(recorded.body())
      recordedJson.get("entries").size() shouldBe 2

      val cashBalance = mapper.readTree(get(s"/accounts/$cashId/balance").body())
      cashBalance.get("amount").decimalValue() shouldBe BigDecimal("-100.0000").bigDecimal

      val revenueBalance = mapper.readTree(get(s"/accounts/$revenueId/balance").body())
      revenueBalance.get("amount").decimalValue() shouldBe BigDecimal("100.0000").bigDecimal

      val byPayment = mapper.readTree(get(s"/entries/$paymentId").body())
      byPayment.get("entries").size() shouldBe 2
    }

    it("returns 200 with existing entries when the same paymentId is recorded twice (idempotent)") {
      val a = createAccount("A", "USD")
      val b = createAccount("B", "USD")
      val paymentId = UUID.randomUUID()
      val body =
        s"""{
           |  "paymentId": "$paymentId",
           |  "legs": [
           |    { "accountId": "$a", "entryType": "DEBIT",  "amount": 50.00, "currency": "USD" },
           |    { "accountId": "$b", "entryType": "CREDIT", "amount": 50.00, "currency": "USD" }
           |  ]
           |}""".stripMargin
      val first = post("/entries", body)
      val second = post("/entries", body)
      first.statusCode() shouldBe 201
      second.statusCode() shouldBe 200

      val firstIds = entryIds(first.body())
      val secondIds = entryIds(second.body())
      secondIds shouldBe firstIds
    }

    it("rejects an unbalanced transaction with 422") {
      val a = createAccount("A", "INR")
      val b = createAccount("B", "INR")
      val body =
        s"""{
           |  "paymentId": "${UUID.randomUUID()}",
           |  "legs": [
           |    { "accountId": "$a", "entryType": "DEBIT",  "amount": 100.00, "currency": "INR" },
           |    { "accountId": "$b", "entryType": "CREDIT", "amount":  90.00, "currency": "INR" }
           |  ]
           |}""".stripMargin
      val resp = post("/entries", body)
      resp.statusCode() shouldBe 422
      mapper.readTree(resp.body()).get("code").asText() shouldBe "invariant_violation"
    }

    it("rejects an entry whose currency does not match the account") {
      val a = createAccount("A", "INR")
      val b = createAccount("B", "INR")
      val body =
        s"""{
           |  "paymentId": "${UUID.randomUUID()}",
           |  "legs": [
           |    { "accountId": "$a", "entryType": "DEBIT",  "amount": 10.00, "currency": "USD" },
           |    { "accountId": "$b", "entryType": "CREDIT", "amount": 10.00, "currency": "USD" }
           |  ]
           |}""".stripMargin
      val resp = post("/entries", body)
      resp.statusCode() shouldBe 422
      mapper.readTree(resp.body()).get("code").asText() shouldBe "invariant_violation"
    }

    it("rejects entries for an unknown account with 404") {
      val a = createAccount("A", "INR")
      val ghost = UUID.randomUUID()
      val body =
        s"""{
           |  "paymentId": "${UUID.randomUUID()}",
           |  "legs": [
           |    { "accountId": "$a",     "entryType": "DEBIT",  "amount": 5.00, "currency": "INR" },
           |    { "accountId": "$ghost", "entryType": "CREDIT", "amount": 5.00, "currency": "INR" }
           |  ]
           |}""".stripMargin
      val resp = post("/entries", body)
      resp.statusCode() shouldBe 404
    }

    it("returns 404 for an unknown account balance") {
      val resp = get(s"/accounts/${UUID.randomUUID()}/balance")
      resp.statusCode() shouldBe 404
    }

    it("returns 400 for a malformed UUID in the path") {
      val resp = get("/accounts/not-a-uuid/balance")
      resp.statusCode() shouldBe 400
      mapper.readTree(resp.body()).get("code").asText() shouldBe "invalid_path_parameter"
    }

    it("rejects an unknown entryType with 400") {
      val a = createAccount("A", "INR")
      val b = createAccount("B", "INR")
      val body =
        s"""{
           |  "paymentId": "${UUID.randomUUID()}",
           |  "legs": [
           |    { "accountId": "$a", "entryType": "FOO", "amount": 1.00, "currency": "INR" },
           |    { "accountId": "$b", "entryType": "BAR", "amount": 1.00, "currency": "INR" }
           |  ]
           |}""".stripMargin
      val resp = post("/entries", body)
      resp.statusCode() shouldBe 400
      mapper.readTree(resp.body()).get("code").asText() shouldBe "invalid_entry_type"
    }

    it("rejects a missing currency on account creation with 400") {
      val resp = post("/accounts", """{"name": "Cash"}""")
      resp.statusCode() shouldBe 400
    }

    it("returns an empty list for an unknown paymentId entry query") {
      val resp = get(s"/entries/${UUID.randomUUID()}")
      resp.statusCode() shouldBe 200
      mapper.readTree(resp.body()).get("entries").size() shouldBe 0
    }
  }

  // ---- helpers ----

  private def createAccount(name: String, currency: String): UUID = {
    val resp = post("/accounts", s"""{"name": "$name", "currency": "$currency"}""")
    resp.statusCode() shouldBe 201
    UUID.fromString(mapper.readTree(resp.body()).get("id").asText())
  }

  private def entryIds(body: String): Set[String] = {
    val arr = mapper.readTree(body).get("entries")
    val ids = scala.collection.mutable.Set.empty[String]
    val it = arr.elements()
    while (it.hasNext) ids += it.next().get("id").asText()
    ids.toSet
  }

  private def post(path: String, body: String): HttpResponse[String] = {
    val req = HttpRequest.newBuilder()
      .uri(URI.create(s"http://localhost:$port$path"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    client.send(req, HttpResponse.BodyHandlers.ofString())
  }

  private def get(path: String): HttpResponse[String] = {
    val req = HttpRequest.newBuilder()
      .uri(URI.create(s"http://localhost:$port$path"))
      .GET()
      .build()
    client.send(req, HttpResponse.BodyHandlers.ofString())
  }
}
