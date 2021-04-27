name := "ot-sitemap"

version := "0.1"

scalaVersion := "2.12.12"

idePackagePrefix := Some("io.opentargets.sitemap")

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-bigquery" % "1.128.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.scalactic" %% "scalactic" % "3.2.7",
  "org.scalatest" %% "scalatest" % "3.2.7" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3"
)
