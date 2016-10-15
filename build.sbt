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
      "org.splink" %% "pagelets" % "0.0.1",
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.webjars" % "bootstrap" % "3.3.4"
    )
  )

LessKeys.compress in Assets := true
LessKeys.optimization in Assets := 100

includeFilter in (Assets, LessKeys.less) := "*.less"
excludeFilter in (Assets, LessKeys.less) := "_*.less"
pipelineStages in Assets := Seq(uglify)


