// Invariant enforcement for the ledger. Guarantees: (1) double-entry — sum of
// debits equals sum of credits per transaction; (2) balance invariant — account
// balance equals sum of entry effects; (3) immutability — ledger records are append-only.
// Responsibility: Enforce accounting guarantees and reject invalid state.

package com.paymentsplatform.ledger.internal.domain
