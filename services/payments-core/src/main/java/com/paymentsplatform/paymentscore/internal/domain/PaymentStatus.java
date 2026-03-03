package com.paymentsplatform.paymentscore.internal.domain;

/**
 * Defines all possible states of a PaymentIntent and the legal transitions between them.
 *
 * State machine:
 *
 *   CREATED ──► PROCESSING ──► SUCCEEDED  (terminal)
 *      │                  └──► FAILED     (terminal)
 *      └──────────────────────► CANCELED  (terminal)
 *
 * Rules:
 * - CREATED can move to PROCESSING or CANCELED.
 * - PROCESSING can move to SUCCEEDED or FAILED.
 * - SUCCEEDED, FAILED, and CANCELED are terminal — no further transitions are allowed.
 */
public enum PaymentStatus {

    CREATED {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return next == PROCESSING || next == CANCELED;
        }
    },

    PROCESSING {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return next == SUCCEEDED || next == FAILED;
        }
    },

    SUCCEEDED {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return false;
        }
    },

    FAILED {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return false;
        }
    },

    CANCELED {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return false;
        }
    };

    /**
     * Returns true if this status can legally transition to the given status.
     */
    public abstract boolean canTransitionTo(PaymentStatus next);

    /**
     * Returns true if this status is a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
}
