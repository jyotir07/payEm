# Terraform input variables. Region, environment, and resource sizing.
# Responsibility: Parameterize infrastructure for staging vs production.

variable "region" {
  description = "AWS region for resources."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (e.g. staging, production)."
  type        = string
}
