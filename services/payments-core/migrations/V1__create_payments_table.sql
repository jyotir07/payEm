-- V1: Create the payments table.
-- Stores all payment intents. The idempotency_key column has a UNIQUE constraint
-- to enforce exactly-once semantics at the database level.

CREATE TABLE IF NOT EXISTS payments (
    id               UUID         PRIMARY KEY,
    idempotency_key  VARCHAR(255) UNIQUE NOT NULL,
    amount_value     NUMERIC(19,4) NOT NULL,
    amount_currency  VARCHAR(3)   NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
