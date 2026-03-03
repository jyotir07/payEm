package com.paymentsplatform.paymentscore.internal.domain;

/**
 * Documents the payment state machine topology.
 *
 * The state machine is implemented directly in {@link PaymentStatus} and {@link PaymentIntent}:
 * - Transition rules live in {@link PaymentStatus#canTransitionTo(PaymentStatus)}.
 * - Transition enforcement lives in the named methods on {@link PaymentIntent}.
 *
 * State machine:
 *
 *   CREATED ──► PROCESSING ──► SUCCEEDED  (terminal)
 *      │                  └──► FAILED     (terminal)
 *      └──────────────────────► CANCELED  (terminal)
 *
 * Legal transitions:
 *   CREATED    → PROCESSING  via PaymentIntent#startProcessing()
 *   CREATED    → CANCELED    via PaymentIntent#cancel()
 *   PROCESSING → SUCCEEDED   via PaymentIntent#markSucceeded()
 *   PROCESSING → FAILED      via PaymentIntent#markFailed()
 *
 * This class is reserved for future use if the state machine grows complex enough
 * to warrant extraction into a standalone orchestrator.
 */
public final class PaymentStateMachine {
    private PaymentStateMachine() {}
}
