// Rate limiting middleware for api-gateway. Enforces per-tenant or per-key
// request limits and returns 429 when exceeded. Responsibility: Throttling and backpressure.

package middleware
