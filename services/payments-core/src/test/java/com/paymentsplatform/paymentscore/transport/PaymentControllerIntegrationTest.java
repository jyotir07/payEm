package com.paymentsplatform.paymentscore.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentsplatform.paymentscore.Application;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end HTTP test of the Phase 3 transport layer against a real Postgres.
 *
 * Boots the full app via Application.buildApp(), starts Javalin on a random port,
 * and exercises every route with the JDK HttpClient.
 */
@Testcontainers
class PaymentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payments_test")
            .withUsername("test")
            .withPassword("test");

    private static DataSource dataSource;
    private static Javalin app;
    private static int port;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeAll
    static void setUp() throws Exception {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(postgres.getJdbcUrl());
        hc.setUsername(postgres.getUsername());
        hc.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(hc);
        applyMigrations();

        app = Application.buildApp(dataSource);
        app.start(0);
        port = app.port();
    }

    @AfterAll
    static void tearDown() {
        if (app != null) app.stop();
    }

    private static void applyMigrations() throws Exception {
        InputStream stream = PaymentControllerIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("V1__create_payments_table.sql");
        assertNotNull(stream, "Migration not on classpath");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // ---- happy path: full create → process → confirm ----

    @Test
    void fullLifecycle_createProcessSucceed() throws Exception {
        String key = uniqueKey("lifecycle");
        HttpResponse<String> create = post("/payments",
                "{\"amount\": 1000.00, \"currency\": \"INR\"}", key);
        assertEquals(201, create.statusCode());

        JsonNode body = mapper.readTree(create.body());
        String id = body.get("id").asText();
        assertEquals("CREATED", body.get("status").asText());
        assertEquals("INR", body.get("currency").asText());

        HttpResponse<String> got = get("/payments/" + id);
        assertEquals(200, got.statusCode());
        assertEquals("CREATED", mapper.readTree(got.body()).get("status").asText());

        HttpResponse<String> processed = post("/payments/" + id + "/process", "", null);
        assertEquals(200, processed.statusCode());
        assertEquals("PROCESSING", mapper.readTree(processed.body()).get("status").asText());

        HttpResponse<String> confirmed = post("/payments/" + id + "/confirm",
                "{\"outcome\": \"succeeded\"}", null);
        assertEquals(200, confirmed.statusCode());
        assertEquals("SUCCEEDED", mapper.readTree(confirmed.body()).get("status").asText());
    }

    @Test
    void cancel_fromCreated_succeeds() throws Exception {
        String id = createPayment("cancel-ok");
        HttpResponse<String> canceled = post("/payments/" + id + "/cancel", "", null);
        assertEquals(200, canceled.statusCode());
        assertEquals("CANCELED", mapper.readTree(canceled.body()).get("status").asText());
    }

    // ---- idempotency: duplicate key returns the existing intent with 200 ----

    @Test
    void create_duplicateIdempotencyKey_returnsExisting() throws Exception {
        String key = uniqueKey("dup");
        HttpResponse<String> first = post("/payments",
                "{\"amount\": 200.00, \"currency\": \"INR\"}", key);
        HttpResponse<String> second = post("/payments",
                "{\"amount\": 9999.99, \"currency\": \"USD\"}", key);

        assertEquals(201, first.statusCode());
        assertEquals(200, second.statusCode());

        JsonNode firstBody = mapper.readTree(first.body());
        JsonNode secondBody = mapper.readTree(second.body());
        assertEquals(firstBody.get("id").asText(), secondBody.get("id").asText());
        assertEquals("INR", secondBody.get("currency").asText());
    }

    // ---- error mapping ----

    @Test
    void process_alreadyTerminal_returns422() throws Exception {
        String id = createPayment("process-twice");
        assertEquals(200, post("/payments/" + id + "/process", "", null).statusCode());
        HttpResponse<String> second = post("/payments/" + id + "/process", "", null);
        assertEquals(422, second.statusCode());
        assertEquals("invalid_state_transition", mapper.readTree(second.body()).get("code").asText());
    }

    @Test
    void cancel_afterProcessing_returns422() throws Exception {
        String id = createPayment("cancel-late");
        assertEquals(200, post("/payments/" + id + "/process", "", null).statusCode());
        HttpResponse<String> canceled = post("/payments/" + id + "/cancel", "", null);
        assertEquals(422, canceled.statusCode());
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        HttpResponse<String> resp = get("/payments/" + UUID.randomUUID());
        assertEquals(404, resp.statusCode());
        assertEquals("not_found", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void get_malformedUuid_returns400() throws Exception {
        HttpResponse<String> resp = get("/payments/not-a-uuid");
        assertEquals(400, resp.statusCode());
        assertEquals("invalid_path_parameter", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void create_missingIdempotencyKeyHeader_returns400() throws Exception {
        HttpResponse<String> resp = post("/payments",
                "{\"amount\": 100.00, \"currency\": \"INR\"}", null);
        assertEquals(400, resp.statusCode());
        assertEquals("missing_idempotency_key", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void create_invalidCurrency_returns400() throws Exception {
        HttpResponse<String> resp = post("/payments",
                "{\"amount\": 100.00, \"currency\": \"INVALID\"}", uniqueKey("bad-currency"));
        assertEquals(400, resp.statusCode());
        assertEquals("invalid_money", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void create_negativeAmount_returns400() throws Exception {
        HttpResponse<String> resp = post("/payments",
                "{\"amount\": -10.00, \"currency\": \"INR\"}", uniqueKey("neg-amount"));
        assertEquals(400, resp.statusCode());
        assertEquals("invalid_money", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void confirm_unknownOutcome_returns400() throws Exception {
        String id = createPayment("bad-outcome");
        post("/payments/" + id + "/process", "", null);
        HttpResponse<String> resp = post("/payments/" + id + "/confirm",
                "{\"outcome\": \"maybe\"}", null);
        assertEquals(400, resp.statusCode());
        assertEquals("invalid_outcome", mapper.readTree(resp.body()).get("code").asText());
    }

    @Test
    void confirm_fromCreated_returns422() throws Exception {
        String id = createPayment("confirm-too-early");
        HttpResponse<String> resp = post("/payments/" + id + "/confirm",
                "{\"outcome\": \"succeeded\"}", null);
        assertEquals(422, resp.statusCode());
    }

    // ---- helpers ----

    private static String createPayment(String label) throws Exception {
        HttpResponse<String> resp = post("/payments",
                "{\"amount\": 100.00, \"currency\": \"INR\"}", uniqueKey(label));
        assertEquals(201, resp.statusCode());
        String id = mapper.readTree(resp.body()).get("id").asText();
        assertNotEquals("", id);
        assertTrue(id.length() > 0);
        return id;
    }

    private static HttpResponse<String> post(String path, String body, String idempotencyKey) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null) b.header("Idempotency-Key", idempotencyKey);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String uniqueKey(String label) {
        return "ctrl-" + label + "-" + UUID.randomUUID();
    }
}
