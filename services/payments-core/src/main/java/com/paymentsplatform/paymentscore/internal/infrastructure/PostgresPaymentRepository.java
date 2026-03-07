package com.paymentsplatform.paymentscore.internal.infrastructure;

import com.paymentsplatform.paymentscore.internal.domain.Money;
import com.paymentsplatform.paymentscore.internal.domain.PaymentIntent;
import com.paymentsplatform.paymentscore.internal.domain.PaymentRepository;
import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-backed implementation of PaymentRepository.
 *
 * Uses plain JDBC — no ORM. The PaymentIntent domain entity has zero persistence annotations.
 * Row mapping is handled manually via PaymentIntent.reconstitute().
 *
 * save() is an upsert: inserts on first call, updates status and updated_at on subsequent calls.
 * The idempotency_key, amount, and created_at columns are immutable after the first insert.
 */
public class PostgresPaymentRepository implements PaymentRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO payments (id, idempotency_key, amount_value, amount_currency, status, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "    status = EXCLUDED.status, " +
            "    updated_at = EXCLUDED.updated_at";

    private static final String FIND_BY_ID_SQL =
            "SELECT id, idempotency_key, amount_value, amount_currency, status, created_at, updated_at " +
            "FROM payments WHERE id = ?";

    private static final String FIND_BY_KEY_SQL =
            "SELECT id, idempotency_key, amount_value, amount_currency, status, created_at, updated_at " +
            "FROM payments WHERE idempotency_key = ?";

    private final DataSource dataSource;

    public PostgresPaymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(PaymentIntent payment) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPSERT_SQL)) {

            stmt.setObject(1, payment.getId());
            stmt.setString(2, payment.getIdempotencyKey());
            stmt.setBigDecimal(3, payment.getAmount().getAmount());
            stmt.setString(4, payment.getAmount().getCurrency());
            stmt.setString(5, payment.getStatus().name());
            stmt.setTimestamp(6, Timestamp.from(payment.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.from(payment.getUpdatedAt()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment " + payment.getId(), e);
        }
    }

    @Override
    public Optional<PaymentIntent> findById(UUID id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by id " + id, e);
        }
    }

    @Override
    public Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_KEY_SQL)) {

            stmt.setString(1, idempotencyKey);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by idempotency key " + idempotencyKey, e);
        }
    }

    private PaymentIntent mapRow(ResultSet rs) throws SQLException {
        return PaymentIntent.reconstitute(
                (UUID) rs.getObject("id"),
                Money.of(rs.getBigDecimal("amount_value"), rs.getString("amount_currency")),
                rs.getString("idempotency_key"),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
