// Application entrypoint for ledger-service. Bootstraps HTTP/gRPC server,
// double-entry bookkeeping, balance invariants, and immutable ledger records.
// Responsibility: Start the service and wire config, API, and domain.

package com.paymentsplatform.ledger

object Main {
  def main(args: Array[String]): Unit = {
    // Bootstrap: load config, start server, register ledger and balance handlers.
  }
}
