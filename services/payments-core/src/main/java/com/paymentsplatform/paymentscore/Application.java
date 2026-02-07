// Application entrypoint for payments-core. Bootstraps HTTP/gRPC server,
// payment intent lifecycle, idempotency, state machines, and event publishing.
// Responsibility: Start the service and wire config, API, and domain.

package com.paymentsplatform.paymentscore;

public class Application {
    public static void main(String[] args) {
        // Bootstrap: load config, start server, register payment and idempotency handlers.
    }
}
