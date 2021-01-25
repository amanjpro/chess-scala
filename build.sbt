organization := "me.amanj"
name := "zahak"

version := "1.0"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-encoding", "utf8", // Option and arguments on same line
  "-Xfatal-warnings",  // New lines for each options
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

fork in Test := true
fork in run := true

javaOptions += "-Xmx2G"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0",
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest-wordspec" % "3.2.0" % "test",
  "com.typesafe" % "config" % "1.4.1"
)
