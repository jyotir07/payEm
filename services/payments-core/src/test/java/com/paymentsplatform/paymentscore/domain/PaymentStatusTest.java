package com.paymentsplatform.paymentscore.domain;

import com.paymentsplatform.paymentscore.internal.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    // -------------------------------------------------------------------------
    // CREATED transitions
    // -------------------------------------------------------------------------

    @Test
    void createdCanTransitionToProcessing() {
        assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.PROCESSING));
    }

    @Test
    void createdCanTransitionToCanceled() {
        assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.CANCELED));
    }

    @Test
    void createdCannotTransitionToSucceeded() {
        assertFalse(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.SUCCEEDED));
    }

    @Test
    void createdCannotTransitionToFailed() {
        assertFalse(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.FAILED));
    }

    @Test
    void createdCannotTransitionToSelf() {
        assertFalse(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.CREATED));
    }

    // -------------------------------------------------------------------------
    // PROCESSING transitions
    // -------------------------------------------------------------------------

    @Test
    void processingCanTransitionToSucceeded() {
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.SUCCEEDED));
    }

    @Test
    void processingCanTransitionToFailed() {
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED));
    }

    @Test
    void processingCannotTransitionToCreated() {
        assertFalse(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.CREATED));
    }

    @Test
    void processingCannotTransitionToCanceled() {
        assertFalse(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.CANCELED));
    }

    @Test
    void processingCannotTransitionToSelf() {
        assertFalse(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.PROCESSING));
    }

    // -------------------------------------------------------------------------
    // Terminal states — SUCCEEDED, FAILED, CANCELED
    // -------------------------------------------------------------------------

    @Test
    void succeededIsTerminalAndBlocksAllTransitions() {
        for (PaymentStatus target : PaymentStatus.values()) {
            assertFalse(
                    PaymentStatus.SUCCEEDED.canTransitionTo(target),
                    "SUCCEEDED should not transition to " + target
            );
        }
    }

    @Test
    void failedIsTerminalAndBlocksAllTransitions() {
        for (PaymentStatus target : PaymentStatus.values()) {
            assertFalse(
                    PaymentStatus.FAILED.canTransitionTo(target),
                    "FAILED should not transition to " + target
            );
        }
    }

    @Test
    void canceledIsTerminalAndBlocksAllTransitions() {
        for (PaymentStatus target : PaymentStatus.values()) {
            assertFalse(
                    PaymentStatus.CANCELED.canTransitionTo(target),
                    "CANCELED should not transition to " + target
            );
        }
    }

    // -------------------------------------------------------------------------
    // isTerminal
    // -------------------------------------------------------------------------

    @Test
    void succeededIsTerminal() {
        assertTrue(PaymentStatus.SUCCEEDED.isTerminal());
    }

    @Test
    void failedIsTerminal() {
        assertTrue(PaymentStatus.FAILED.isTerminal());
    }

    @Test
    void canceledIsTerminal() {
        assertTrue(PaymentStatus.CANCELED.isTerminal());
    }

    @Test
    void createdIsNotTerminal() {
        assertFalse(PaymentStatus.CREATED.isTerminal());
    }

    @Test
    void processingIsNotTerminal() {
        assertFalse(PaymentStatus.PROCESSING.isTerminal());
    }
}
