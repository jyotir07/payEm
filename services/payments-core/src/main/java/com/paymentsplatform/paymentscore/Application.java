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
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresIdempotencyStore;
import com.paymentsplatform.paymentscore.internal.infrastructure.PostgresPaymentRepository;
import com.paymentsplatform.paymentscore.internal.transport.ErrorHandler;
import com.paymentsplatform.paymentscore.internal.transport.PaymentController;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.sql.DataSource;

/**
 * Application entrypoint for payments-core.
 *
 * Wires the dependency graph by hand — no DI framework. Order:
 *   DataSource → repository + idempotency store → use cases → controller → Javalin
 */
public class Application {

    public static void main(String[] args) {
        DataSource dataSource = buildDataSource();
        Javalin app = buildApp(dataSource);
        int port = Integer.parseInt(envOrDefault("PORT", "8080"));
        app.start(port);
    }

    /**
     * Builds the fully-wired Javalin app. Exposed for tests so they can mount the
     * same router against a Testcontainers-backed DataSource without booting main().
     */
    public static Javalin buildApp(DataSource dataSource) {
        PaymentRepository repository = new PostgresPaymentRepository(dataSource);
        IdempotencyStore idempotencyStore = new PostgresIdempotencyStore(dataSource);

        CreatePaymentUseCase create = new CreatePaymentUseCase(repository, idempotencyStore);
        GetPaymentUseCase get = new GetPaymentUseCase(repository);
        ProcessPaymentUseCase process = new ProcessPaymentUseCase(repository);
        ConfirmPaymentUseCase confirm = new ConfirmPaymentUseCase(repository);
        CancelPaymentUseCase cancel = new CancelPaymentUseCase(repository);

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

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
