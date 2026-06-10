-- Ledger schema. Append-only: no updated_at, no soft delete column.

CREATE TABLE IF NOT EXISTS accounts (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id              UUID            PRIMARY KEY,
    account_id      UUID            NOT NULL REFERENCES accounts(id),
    entry_type      VARCHAR(8)      NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount_value    NUMERIC(19, 4)  NOT NULL,
    amount_currency VARCHAR(3)      NOT NULL,
    payment_id      UUID            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX IF NOT EXISTS ledger_entries_account_idx ON ledger_entries (account_id);
CREATE INDEX IF NOT EXISTS ledger_entries_payment_idx ON ledger_entries (payment_id);
