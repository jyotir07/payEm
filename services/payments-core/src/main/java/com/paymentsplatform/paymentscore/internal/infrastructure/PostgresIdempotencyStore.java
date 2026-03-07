package com.paymentsplatform.paymentscore.internal.infrastructure;

import com.paymentsplatform.paymentscore.internal.domain.IdempotencyStore;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

/**
 * IdempotencyStore backed by the payments table.
 *
 * exists() queries the payments table by idempotency_key.
 * save() is a no-op: the idempotency key is already written when
 * PostgresPaymentRepository.save() inserts the payment row.
 */
public class PostgresIdempotencyStore implements IdempotencyStore {

    private static final String EXISTS_SQL =
            "SELECT 1 FROM payments WHERE idempotency_key = ? LIMIT 1";

    private final DataSource dataSource;

    public PostgresIdempotencyStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean exists(String key) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS_SQL)) {

            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check idempotency key: " + key, e);
        }
    }

    @Override
    public void save(String key, UUID paymentId) {
        // The idempotency key is stored as part of the payment row (idempotency_key column).
        // PostgresPaymentRepository.save() already persists it — no separate write needed here.
    }
}
