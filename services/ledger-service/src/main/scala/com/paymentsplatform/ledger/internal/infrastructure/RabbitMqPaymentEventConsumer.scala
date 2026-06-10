package com.paymentsplatform.ledger.internal.infrastructure

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.paymentsplatform.ledger.internal.application.RecordTransactionUseCase
import com.paymentsplatform.ledger.internal.domain.EntryType
import com.rabbitmq.client.{Channel, Connection, DefaultConsumer, Envelope, AMQP}

import java.util.UUID

/**
  * Consumes PaymentSucceeded events from the `payments.events` topic exchange and
  * records a balanced debit/credit pair against the configured cash and revenue
  * accounts via `RecordTransactionUseCase`. Consumption is idempotent on
  * `paymentId` thanks to `RecordTransactionUseCase.execute`'s short-circuit on
  * `findEntriesByPayment`.
  *
  * The wire format is the JSON envelope described in
  * `libs/contracts/events/README.md`.
  */
final class RabbitMqPaymentEventConsumer(
    connection: Connection,
    mapper: ObjectMapper,
    recordTransaction: RecordTransactionUseCase,
    cashAccountId: UUID,
    revenueAccountId: UUID
) extends AutoCloseable {

  import RabbitMqPaymentEventConsumer._

  private val channel: Channel = connection.createChannel()
  channel.exchangeDeclare(Exchange, "topic", true)
  channel.queueDeclare(Queue, true, false, false, null)
  channel.queueBind(Queue, Exchange, RoutingKey)

  def start(): Unit = {
    channel.basicConsume(
      Queue,
      false,
      new DefaultConsumer(channel) {
        override def handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: Array[Byte]
        ): Unit = {
          val deliveryTag = envelope.getDeliveryTag
          try {
            val node = mapper.readTree(body)
            handle(node)
            channel.basicAck(deliveryTag, false)
          } catch {
            case t: Throwable =>
              System.err.println(
                s"[ledger-service] consume failed (tag=$deliveryTag): ${t.getMessage}"
              )
              // requeue=false: don't poison-loop a malformed message; the broker can
              // be wired to a DLX later (Phase 7+).
              channel.basicNack(deliveryTag, false, false)
          }
        }
      }
    )
  }

  private def handle(node: JsonNode): Unit = {
    val paymentId = UUID.fromString(node.get("paymentId").asText())
    val amount    = BigDecimal(node.get("amount").asText())
    val currency  = node.get("currency").asText()

    val request = RecordTransactionUseCase.Request(
      paymentId = paymentId,
      legs = List(
        RecordTransactionUseCase.Leg(cashAccountId, EntryType.Debit, amount, currency),
        RecordTransactionUseCase.Leg(revenueAccountId, EntryType.Credit, amount, currency)
      )
    )
    recordTransaction.execute(request)
  }

  override def close(): Unit = {
    try channel.close() catch { case _: Throwable => }
  }
}

object RabbitMqPaymentEventConsumer {
  val Exchange   = "payments.events"
  val Queue      = "ledger.payment-events"
  val RoutingKey = "payment.succeeded"
}
