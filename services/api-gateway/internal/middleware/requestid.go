// Request ID and context propagation middleware. Generates or forwards
// X-Request-ID, injects into context, and propagates to downstream services.
// Responsibility: Request tracing and context propagation across service boundaries.

package middleware
