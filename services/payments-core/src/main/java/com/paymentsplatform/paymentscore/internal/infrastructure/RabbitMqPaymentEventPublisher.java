package com.paymentsplatform.paymentscore.internal.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RabbitMQ-backed publisher. Declares the topic exchange on construction and
 * publishes JSON envelopes that match libs/contracts/events/README.md.
 *
 * <p>Failures are swallowed and logged to stderr — see the publish-after-commit
 * contract: we never want a broker hiccup to undo a committed payment write.
 * True at-least-once delivery would need a transactional outbox, which is a
 * later hardening step.
 */
public final class RabbitMqPaymentEventPublisher implements PaymentEventPublisher, AutoCloseable {

    public static final String EXCHANGE = "payments.events";

    private final Connection connection;
    private final Channel channel;
    private final ObjectMapper mapper;
    private final Object lock = new Object();

    public RabbitMqPaymentEventPublisher(Connection connection, ObjectMapper mapper) {
        this.connection = connection;
        this.mapper = mapper;
        try {
            this.channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE, "topic", true);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to declare exchange " + EXCHANGE, e);
        }
    }

    @Override
    public void publish(PaymentEvent event) {
        try {
            byte[] body = mapper.writeValueAsBytes(envelope(event));
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding("utf-8")
                    .deliveryMode(MessageProperties.PERSISTENT_TEXT_PLAIN.getDeliveryMode())
                    .type(event.eventType())
                    .build();
            synchronized (lock) {
                channel.basicPublish(EXCHANGE, event.routingKey(), props, body);
            }
        } catch (Exception e) {
            System.err.println("[payments-core] publish failed for "
                    + event.eventType() + " paymentId=" + event.paymentId()
                    + ": " + e.getMessage());
        }
    }

    private Map<String, Object> envelope(PaymentEvent event) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventType", event.eventType());
        m.put("schemaVersion", event.schemaVersion());
        m.put("paymentId", event.paymentId().toString());
        m.put("amount", event.amount().toPlainString());
        m.put("currency", event.currency());
        m.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(event.timestamp()));
        return m;
    }

    @Override
    public void close() {
        try { channel.close(); } catch (Exception ignored) { /* shutdown best-effort */ }
        try { connection.close(); } catch (Exception ignored) { /* shutdown best-effort */ }
    }
}
