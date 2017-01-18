name := """raywiki"""
organization := "com.raybeam"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.planet42" %% "laika-core" % "0.6.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "2.6.3" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.raybeam.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.raybeam.binders._"
