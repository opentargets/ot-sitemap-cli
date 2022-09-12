name := "ot-sitemap"
organization := "io.opentargets"
version := "1.1"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  // Google Cloud Platform
  "com.google.cloud" % "google-cloud-bigquery" % "1.128.1",
  "com.google.cloud" % "google-cloud-storage" % "1.113.15",
  // https://mvnrepository.com/artifact/com.google.cloud/google-cloud-resourcemanager
  "com.google.cloud" % "google-cloud-resourcemanager" % "0.118.12-alpha",
// Test
  "org.scalactic" %% "scalactic" % "3.2.7",
  "org.scalatest" %% "scalatest" % "3.2.7" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.7.0" % "test",
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
  // command line interface
  "com.github.scopt" % "scopt_2.12" % "4.0.1"
)

// Jar assembly: run `sbt assembly` to create fat jar
mainClass in assembly := Some("io.opentargets.sitemap.Main")
assemblyJarName in assembly := "ot-sitemap.jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _                             => MergeStrategy.first
}
