// State machine for payment intent lifecycle. Defines valid transitions
// and guards (e.g. created -> processing -> succeeded).
// Responsibility: Enforce valid payment state transitions.

package com.paymentsplatform.paymentscore.internal.domain;
