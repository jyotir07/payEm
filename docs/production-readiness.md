# Production Readiness

Deployment and safety practices for the payments platform.

## Deployment

Containers for all services; images from service Dockerfiles. Kubernetes manifests in infra/k8s; Terraform in infra/terraform. Secrets via env or secret managers; never committed.

## Safety

Idempotency for mutating operations. Webhook retry with backoff and dead-letter. Observability: logging, metrics, tracing (libs/observability).
