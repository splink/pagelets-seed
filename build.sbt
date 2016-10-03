import sbt.Keys._

name := """pagelets-seed"""

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).
  settings(Seq(
    scalaVersion := "2.11.8",
    routesImport += "org.splink.pagelets.Binders._"),
    libraryDependencies ++= Seq(
      cache,
      ws,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.splink" %% "pagelets" % "0.0.1-SNAPSHOT",
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
    )
  )



