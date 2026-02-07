// sbt build configuration for ledger-service. Defines Scala version,
// dependencies, and tasks. Responsibility: Language-specific build.

name := "ledger-service"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  // HTTP, DB, and runtime deps
)

enablePlugins(JavaAppPackaging)
enablePlugins(AssemblyPlugin)
assembly / mainClass := Some("com.paymentsplatform.ledger.Main")
