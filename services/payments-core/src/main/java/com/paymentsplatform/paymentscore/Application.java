package com.paymentsplatform.paymentscore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentsplatform.paymentscore.internal.application.CancelPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.ConfirmPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.CreatePaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.GetPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.application.ProcessPaymentUseCase;
import com.paymentsplatform.paymentscore.internal.domain.IdempotencyStore;
import com.paymentsplatform.paymentscore.internal.domain.PaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.infrastructure.NoopPaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresIdempotencyStore;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresPaymentRepository;
import com.paymentsplatform.paymentscore.internal.infrastructure.RabbitMqPaymentEventPublisher;
import com.paymentsplatform.paymentscore.internal.transport.ErrorHandler;
import com.paymentsplatform.paymentscore.internal.transport.PaymentController;
import com.rabbitmq.client.ConnectionFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

/**
 * Application entrypoint for payments-core.
 *
 * Wires the dependency graph by hand — no DI framework. Order:
 *   DataSource → repository + idempotency store → event publisher → use cases → controller → Javalin
 */
public class Application {

    public static void main(String[] args) {
        DataSource dataSource = buildDataSource();
        PaymentEventPublisher publisher = buildEventPublisher();
        Javalin app = buildApp(dataSource, publisher);
        int port = Integer.parseInt(envOrDefault("PORT", "8080"));
        app.start(port);
    }

    /**
     * Default buildApp — used by existing integration tests that don't need a broker.
     * Falls back to a no-op publisher so legacy callers keep working.
     */
    public static Javalin buildApp(DataSource dataSource) {
        return buildApp(dataSource, new NoopPaymentEventPublisher());
    }

    /**
     * Fully-wired Javalin app with an explicit event publisher. Exposed for tests so
     * they can mount the same router against a Testcontainers-backed DataSource and
     * a Testcontainers-backed RabbitMQ.
     */
    public static Javalin buildApp(DataSource dataSource, PaymentEventPublisher publisher) {
        PaymentRepository repository = new PostgresPaymentRepository(dataSource);
        IdempotencyStore idempotencyStore = new PostgresIdempotencyStore(dataSource);

        CreatePaymentUseCase create = new CreatePaymentUseCase(repository, idempotencyStore, publisher);
        GetPaymentUseCase get = new GetPaymentUseCase(repository);
        ProcessPaymentUseCase process = new ProcessPaymentUseCase(repository, publisher);
        ConfirmPaymentUseCase confirm = new ConfirmPaymentUseCase(repository, publisher);
        CancelPaymentUseCase cancel = new CancelPaymentUseCase(repository, publisher);

        PaymentController controller = new PaymentController(create, get, process, confirm, cancel);

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(buildObjectMapper(), true)));
        controller.register(app);
        ErrorHandler.register(app);
        return app;
    }

    private static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static DataSource buildDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(envOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/payments"));
        config.setUsername(envOrDefault("DB_USER", "payments"));
        config.setPassword(envOrDefault("DB_PASSWORD", "payments"));
        return new HikariDataSource(config);
    }

    /**
     * Connects to RabbitMQ at RABBITMQ_URL if set; otherwise falls back to a no-op
     * publisher so the service still boots in environments without a broker.
     */
    private static PaymentEventPublisher buildEventPublisher() {
        String url = System.getenv("RABBITMQ_URL");
        if (url == null || url.isBlank()) {
            System.out.println("[payments-core] RABBITMQ_URL not set, using no-op event publisher");
            return new NoopPaymentEventPublisher();
        }
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(url);
            return new RabbitMqPaymentEventPublisher(factory.newConnection(), buildObjectMapper());
        } catch (Exception e) {
            System.err.println("[payments-core] Failed to connect to RabbitMQ at " + url
                    + " — falling back to no-op publisher: " + e.getMessage());
            return new NoopPaymentEventPublisher();
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
