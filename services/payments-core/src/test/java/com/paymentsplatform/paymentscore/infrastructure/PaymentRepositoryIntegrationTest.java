package com.paymentsplatform.paymentscore.infrastructure;

import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresIdempotencyStore;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresPaymentRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class PaymentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payments_test")
            .withUsername("test")
            .withPassword("test");

    private static DataSource dataSource;
    private PostgresPaymentRepository repository;
    private PostgresIdempotencyStore idempotencyStore;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(config);
        applyMigrations();
    }

    private static void applyMigrations() throws Exception {
        InputStream stream = PaymentRepositoryIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("V1__create_payments_table.sql");
        assertNotNull(stream, "Migration file V1__create_payments_table.sql not found on classpath");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new PostgresPaymentRepository(dataSource);
        idempotencyStore = new PostgresIdempotencyStore(dataSource);
    }

    // --- PaymentRepository: round-trip tests ---

    @Test
    void saveAndFindById_roundTrip() {
        PaymentIntent payment = PaymentIntent.create(
                Money.of(new BigDecimal("1000.00"), "INR"),
                uniqueKey("find-by-id")
        );

        repository.save(payment);

        Optional<PaymentIntent> found = repository.findById(payment.getId());
        assertTrue(found.isPresent());

        PaymentIntent loaded = found.get();
        assertEquals(payment.getId(), loaded.getId());
        assertEquals(payment.getAmount(), loaded.getAmount());
        assertEquals(payment.getIdempotencyKey(), loaded.getIdempotencyKey());
        assertEquals(PaymentStatus.CREATED, loaded.getStatus());
        assertEquals(payment.getCreatedAt().getEpochSecond(), loaded.getCreatedAt().getEpochSecond());
    }

    @Test
    void saveAndFindByIdempotencyKey_roundTrip() {
        String key = uniqueKey("find-by-key");
        PaymentIntent payment = PaymentIntent.create(
                Money.of(new BigDecimal("500.00"), "USD"),
                key
        );

        repository.save(payment);

        Optional<PaymentIntent> found = repository.findByIdempotencyKey(key);
        assertTrue(found.isPresent());
        assertEquals(payment.getId(), found.get().getId());
    }

    @Test
    void save_updatesStatusOnTransition() {
        PaymentIntent payment = PaymentIntent.create(
                Money.of(new BigDecimal("250.00"), "EUR"),
                uniqueKey("status-update")
        );
        repository.save(payment);

        payment.startProcessing();
        repository.save(payment);

        Optional<PaymentIntent> found = repository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.PROCESSING, found.get().getStatus());
    }

    @Test
    void save_persists_fullLifecycle_INR() {
        PaymentIntent payment = PaymentIntent.create(
                Money.of(new BigDecimal("9999.99"), "INR"),
                uniqueKey("lifecycle-inr")
        );
        repository.save(payment);

        payment.startProcessing();
        repository.save(payment);

        payment.markSucceeded();
        repository.save(payment);

        Optional<PaymentIntent> found = repository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, found.get().getStatus());
    }

    @Test
    void findById_returnsEmpty_forUnknownId() {
        Optional<PaymentIntent> result = repository.findById(UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdempotencyKey_returnsEmpty_forUnknownKey() {
        Optional<PaymentIntent> result = repository.findByIdempotencyKey("no-such-key-" + UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    // --- IdempotencyStore tests ---

    @Test
    void idempotencyStore_existsReturnsFalse_beforeSave() {
        String key = uniqueKey("idem-before-save");
        assertFalse(idempotencyStore.exists(key));
    }

    @Test
    void idempotencyStore_existsReturnsTrue_afterRepositorySave() {
        String key = uniqueKey("idem-after-save");
        PaymentIntent payment = PaymentIntent.create(
                Money.of(new BigDecimal("100.00"), "INR"),
                key
        );

        assertFalse(idempotencyStore.exists(key));
        repository.save(payment);
        assertTrue(idempotencyStore.exists(key));
    }

    // --- helpers ---

    private static String uniqueKey(String label) {
        return "test-" + label + "-" + UUID.randomUUID();
    }
}
