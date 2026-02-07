# Ledger Invariants

Accounting guarantees enforced by ledger-service.

## Double-Entry

Every transaction: sum of debits equals sum of credits. No orphan entries.

## Balance Invariant

Account balance equals sum of entry effects. Enforced after every post.

## Immutability

Ledger entries are append-only. Corrections via compensating entries only.
