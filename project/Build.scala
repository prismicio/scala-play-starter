import sbt._, Keys._
import play._

object ApplicationBuild extends Build {

  val appName         = "prismicio-starter"
  val appVersion      = "1.1"

  val main = Project(appName, file(".")) enablePlugins PlayScala settings (

    version := appVersion,
    scalaVersion := "2.11.1",

    // Prismic.io Maven repository
    resolvers += "Prismic.io kits" at "https://github.com/prismicio/repository/raw/master/maven/",

    // The Scala kit
    libraryDependencies += "io.prismic" %% "scala-kit" % "1.0-M16"
  )

}
