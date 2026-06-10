// sbt build configuration for ledger-service. Defines Scala version,
// dependencies, and tasks. Responsibility: Language-specific build.

name := "ledger-service"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  // Phase 5: PostgreSQL JDBC + connection pool
  "org.postgresql"        %  "postgresql"                    % "42.7.3",
  "com.zaxxer"            %  "HikariCP"                      % "5.1.0",

  // Phase 5: HTTP server (Javalin) + JSON (Jackson, with Scala module)
  "io.javalin"            %  "javalin"                       % "6.3.0",
  "com.fasterxml.jackson.core"     % "jackson-databind"      % "2.17.2",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.17.2",
  "com.fasterxml.jackson.module"   %% "jackson-module-scala" % "2.17.2",
  "org.slf4j"             %  "slf4j-simple"                  % "2.0.13",

  // Phase 6: RabbitMQ client for consuming payment events
  "com.rabbitmq"          %  "amqp-client"                   % "5.21.0",

  // Phase 4: tests
  "org.scalatest"         %% "scalatest"                     % "3.2.18"   % Test,

  // Phase 5: Testcontainers Postgres for integration tests
  "org.testcontainers"    %  "postgresql"                    % "1.19.8"   % Test,

  // Phase 6: Testcontainers RabbitMQ for consumer integration test
  "org.testcontainers"    %  "rabbitmq"                      % "1.19.8"   % Test
)

// Expose migrations on the test classpath so integration tests can apply them.
Compile / unmanagedResourceDirectories += baseDirectory.value / "migrations"

// Pin the JVM timezone so Postgres doesn't reject deprecated zone names
// like "Asia/Calcutta" picked up from the host (Windows "India Standard Time").
Test / javaOptions += "-Duser.timezone=UTC"
Test / fork := true

enablePlugins(JavaAppPackaging)
enablePlugins(AssemblyPlugin)
assembly / mainClass := Some("com.paymentsplatform.ledger.Main")

// Standard assembly merge strategy — drop conflicting service files.
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _                            => MergeStrategy.first
}
