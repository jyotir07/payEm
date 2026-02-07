# Terraform outputs. Expose endpoint URLs, cluster name, and other
# values for CI or downstream config. Responsibility: Output key resource identifiers.

output "api_gateway_endpoint" {
  description = "API Gateway endpoint URL."
  value       = "placeholder"
}
