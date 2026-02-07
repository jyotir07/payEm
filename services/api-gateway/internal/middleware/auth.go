// Authentication middleware for api-gateway. Validates API keys or tokens,
// extracts identity, and attaches user/tenant to request context.
// Responsibility: Authentication and context enrichment before handlers.

package middleware
