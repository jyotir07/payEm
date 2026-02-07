# Terraform root module. Provider and backend configuration, and
# orchestration of modules (e.g. network, database, k8s).
# Responsibility: Entrypoint for terraform apply.

terraform {
  required_version = ">= 1.5"
  # backend "s3" { ... }
}

provider "aws" {
  region = var.region
}

# Placeholder resources or module calls
