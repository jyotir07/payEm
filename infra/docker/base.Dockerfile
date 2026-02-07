# Base image placeholder for payments-platform services. Common layers
# (e.g. ca-certificates, user) shared by service Dockerfiles.
# Responsibility: Reduce duplication and standardize base.

FROM alpine:3.19
RUN apk --no-cache add ca-certificates
# Add non-root user if required
