# Service container definition for payments-core. Multi-stage build:
# build stage compiles Java app; final stage runs the JAR.
# Responsibility: Production and local dev image for payments-core.

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
