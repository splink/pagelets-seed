import sbt.Keys._

name := """pagelets-seed"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala).
  settings(Seq(
    scalaVersion := "2.13.6",
    routesImport += "org.splink.pagelets.Binders._"),
    libraryDependencies ++= Seq(
      ws,
      guice,
      "org.splink" %% "pagelets" % "0.0.10",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
      "org.mockito" % "mockito-scala-scalatest_2.13" % "1.16.46" % Test,
      "org.scalamock" %% "scalamock" % "4.4.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.webjars.bower" % "bootstrap" % "3.3.7",
      "org.webjars" % "jquery" % "3.1.1"
    )
  )

// to use streaming, HtmlStreamFormat is required
TwirlKeys.templateFormats ++= Map(
  "stream" -> "org.splink.pagelets.twirl.HtmlStreamFormat")

// auto-import the following types in templates
TwirlKeys.templateImports ++= Vector(
  "org.splink.pagelets.twirl.HtmlStream",
  "org.splink.pagelets.twirl.HtmlStreamFormat",
  "org.splink.pagelets.twirl.HtmlPageStream",
  "org.splink.pagelets.Page")


// enable asset minification
Assets / LessKeys.compress := true
Assets / LessKeys.optimization := 100

uglify / includeFilter := GlobFilter("*.js")
// also minify the bootstrap javascript files
uglify / excludeFilter := new SimpleFileFilter(f =>
  !f.getPath.contains("lib/bootstrap/js") &&
    !f.getPath.contains("assets/javascripts"))

includeFilter in (Assets, LessKeys.less) := "*.less"
excludeFilter in (Assets, LessKeys.less) := "_*.less"

Assets / pipelineStages := Seq(uglify)
