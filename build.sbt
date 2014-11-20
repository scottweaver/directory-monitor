name := "directory-monitor"

version := "1.0"

scalaVersion := "2.11.4"

val akkaVersion = "2.3.6"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test",
  "joda-time" % "joda-time" % "2.5",
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe.akka" %% "akka-actor"      % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"    % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"    % akkaVersion
)

