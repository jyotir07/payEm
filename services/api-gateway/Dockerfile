# Service container definition for api-gateway. Multi-stage build:
# build stage compiles Go binary; final stage runs the binary.
# Responsibility: Production and local dev image for the gateway.

FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /api-gateway ./cmd/api-gateway

FROM alpine:3.19
RUN apk --no-cache add ca-certificates
COPY --from=builder /api-gateway /api-gateway
EXPOSE 8080
ENTRYPOINT ["/api-gateway"]
