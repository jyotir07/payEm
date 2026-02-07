// HTTP handlers for api-gateway. Route incoming requests to downstream
// services (payments-core, ledger-service, webhook-service) and return responses.
// Responsibility: Request/response handling and delegation to client stubs.

package api
