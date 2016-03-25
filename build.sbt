name := "prismicio-starter"

version := "1.2"

scalaVersion := "2.11.7"

libraryDependencies += "io.prismic" %% "scala-kit" % "1.3.7"

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayScala)
