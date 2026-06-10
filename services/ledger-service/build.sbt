// sbt build configuration for ledger-service. Defines Scala version,
// dependencies, and tasks. Responsibility: Language-specific build.

name := "ledger-service"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

enablePlugins(JavaAppPackaging)
enablePlugins(AssemblyPlugin)
assembly / mainClass := Some("com.paymentsplatform.ledger.Main")
