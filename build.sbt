name := "prismicio-starter"

version := "1.1"

scalaVersion := "2.11.1"

libraryDependencies += "io.prismic" %% "scala-kit" % "1.0.16"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

