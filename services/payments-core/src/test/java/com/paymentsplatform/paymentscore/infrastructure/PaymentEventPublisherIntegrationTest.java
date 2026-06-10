package com.paymentsplatform.paymentscore.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentsplatform.paymentscore.Application;
import com.paymentsplatform.paymentscore.internal.infrastructure.RabbitMqPaymentEventPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Boots payments-core HTTP + Postgres + RabbitMQ via Testcontainers and verifies that
 * each mutating use case publishes the right JSON event envelope to the topic exchange.
 */
@Testcontainers
class PaymentEventPublisherIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payments_test").withUsername("test").withPassword("test");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    private static DataSource dataSource;
    private static Javalin app;
    private static int port;
    private static Connection rabbitConnection;
    private static Channel testChannel;
    private static RabbitMqPaymentEventPublisher publisher;
    private static final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeAll
    static void setUp() throws Exception {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(postgres.getJdbcUrl());
        hc.setUsername(postgres.getUsername());
        hc.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(hc);
        applyMigrations();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        factory.setUsername(rabbit.getAdminUsername());
        factory.setPassword(rabbit.getAdminPassword());
        rabbitConnection = factory.newConnection();
        publisher = new RabbitMqPaymentEventPublisher(rabbitConnection, mapper);

        testChannel = rabbitConnection.createChannel();
        String queue = testChannel.queueDeclare().getQueue();
        testChannel.queueBind(queue, RabbitMqPaymentEventPublisher.EXCHANGE, "payment.#");
        DeliverCallback handler = (tag, delivery) ->
                received.offer(mapper.readTree(delivery.getBody()));
        testChannel.basicConsume(queue, true, handler, tag -> {});

        app = Application.buildApp(dataSource, publisher);
        app.start(0);
        port = app.port();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (app != null) app.stop();
        if (publisher != null) publisher.close();
    }

    private static void applyMigrations() throws Exception {
        InputStream stream = PaymentEventPublisherIntegrationTest.class
                .getClassLoader().getResourceAsStream("V1__create_payments_table.sql");
        assertNotNull(stream, "Migration not on classpath");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Test
    void create_then_process_then_confirm_publishesThreeEventsInOrder() throws Exception {
        received.clear();
        String idempotencyKey = "evt-happy-" + UUID.randomUUID();

        // Create
        HttpResponse<String> create = postJson("/payments",
                "{\"amount\":\"1000.00\",\"currency\":\"INR\"}", idempotencyKey);
        assertEquals(201, create.statusCode());
        String paymentId = mapper.readTree(create.body()).get("id").asText();
        JsonNode createdEvent = poll();
        assertEnvelope(createdEvent, "PaymentCreated", paymentId, "1000.00", "INR");

        // Process
        HttpResponse<String> processed = postJson("/payments/" + paymentId + "/process", "", null);
        assertEquals(200, processed.statusCode());
        JsonNode processingEvent = poll();
        assertEnvelope(processingEvent, "PaymentProcessing", paymentId, "1000.00", "INR");

        // Confirm succeeded
        HttpResponse<String> confirmed = postJson("/payments/" + paymentId + "/confirm",
                "{\"outcome\":\"succeeded\"}", null);
        assertEquals(200, confirmed.statusCode());
        JsonNode succeededEvent = poll();
        assertEnvelope(succeededEvent, "PaymentSucceeded", paymentId, "1000.00", "INR");
    }

    @Test
    void confirm_failed_publishesPaymentFailedEvent() throws Exception {
        received.clear();
        String paymentId = createAndProcess("evt-failed-" + UUID.randomUUID(), "250.00", "USD");
        drainOne(); // PaymentCreated
        drainOne(); // PaymentProcessing

        HttpResponse<String> confirmed = postJson("/payments/" + paymentId + "/confirm",
                "{\"outcome\":\"failed\"}", null);
        assertEquals(200, confirmed.statusCode());
        JsonNode event = poll();
        assertEnvelope(event, "PaymentFailed", paymentId, "250.00", "USD");
    }

    @Test
    void cancel_publishesPaymentCanceledEvent() throws Exception {
        received.clear();
        String idempotencyKey = "evt-cancel-" + UUID.randomUUID();
        HttpResponse<String> create = postJson("/payments",
                "{\"amount\":\"42.00\",\"currency\":\"INR\"}", idempotencyKey);
        assertEquals(201, create.statusCode());
        String paymentId = mapper.readTree(create.body()).get("id").asText();
        drainOne(); // PaymentCreated

        HttpResponse<String> canceled = postJson("/payments/" + paymentId + "/cancel", "", null);
        assertEquals(200, canceled.statusCode());
        JsonNode event = poll();
        assertEnvelope(event, "PaymentCanceled", paymentId, "42.00", "INR");
    }

    @Test
    void idempotentReplayOfCreate_doesNotRepublish() throws Exception {
        received.clear();
        String idempotencyKey = "evt-replay-" + UUID.randomUUID();
        String body = "{\"amount\":\"77.00\",\"currency\":\"INR\"}";

        HttpResponse<String> first = postJson("/payments", body, idempotencyKey);
        assertEquals(201, first.statusCode());
        poll(); // consume PaymentCreated

        HttpResponse<String> second = postJson("/payments", body, idempotencyKey);
        assertEquals(200, second.statusCode());

        // Wait briefly to ensure no second event arrives.
        JsonNode extra = received.poll(500, TimeUnit.MILLISECONDS);
        assertNull(extra, "Idempotent replay should not republish; got " + extra);
    }

    // ---- helpers ----

    private static String createAndProcess(String idempotencyKey, String amount, String currency) throws Exception {
        HttpResponse<String> create = postJson("/payments",
                "{\"amount\":\"" + amount + "\",\"currency\":\"" + currency + "\"}", idempotencyKey);
        String paymentId = mapper.readTree(create.body()).get("id").asText();
        postJson("/payments/" + paymentId + "/process", "", null);
        return paymentId;
    }

    private static HttpResponse<String> postJson(String path, String body, String idempotencyKey) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        if (idempotencyKey != null) b.header("Idempotency-Key", idempotencyKey);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode poll() throws InterruptedException {
        JsonNode node = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(node, "Expected an event but none arrived in 5s");
        return node;
    }

    private static void drainOne() throws InterruptedException {
        received.poll(5, TimeUnit.SECONDS);
    }

    private static void assertEnvelope(JsonNode node, String eventType, String paymentId,
                                       String amount, String currency) {
        assertEquals(eventType, node.get("eventType").asText());
        assertEquals(1, node.get("schemaVersion").asInt());
        assertEquals(paymentId, node.get("paymentId").asText());
        assertEquals(amount, node.get("amount").asText());
        assertEquals(currency, node.get("currency").asText());
        assertNotNull(node.get("timestamp").asText());
    }
}
