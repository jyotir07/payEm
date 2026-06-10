package com.paymentsplatform.paymentscore.internal.domain;

import com.paymentsplatform.paymentscore.internal.domain.events.PaymentEvent;

/**
 * Boundary the application layer crosses to fan a domain event out to the world.
 * Implementations live in infrastructure (RabbitMQ today; a noop for tests/dev).
 *
 * <p>Implementations MUST NOT throw on transport failure — see the publish-after-commit
 * contract documented in libs/contracts/events/README.md. Use cases call this after a
 * successful DB write; a publish exception bubbling here would leave the system in a
 * confusing half-committed state from the client's perspective.
 */
public interface PaymentEventPublisher {
    void publish(PaymentEvent event);
}
