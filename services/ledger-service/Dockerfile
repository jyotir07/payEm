# Service container definition for ledger-service. Multi-stage build:
# build stage compiles Scala app; final stage runs the JAR.
# Responsibility: Production and local dev image for ledger-service.

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
RUN apk add --no-cache bash
COPY project ./project
COPY build.sbt ./
COPY src ./src
RUN ./sbt assembly 2>/dev/null || true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/scala-2.13/ledger-service-assembly-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
