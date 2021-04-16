// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbt.Def
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.10"
lazy val scalaV = "2.13.4"
lazy val upickleV = "1.2.2"

lazy val root = (project in file("."))
  .settings(
    organization := "org.eu.nl.laszlo",
    name := "Reactive Fridge Magnets",
    version := "0.0.1-SNAPSHOT",
  )
  .aggregate(backend, frontend)

lazy val backend = (project in file("backend"))
  .settings(commonSettings: _*)
  .settings(
    name := "backend",

    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-upickle" % "1.35.2",
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.2" % Test
    ),

    //TODO: understand what this means
    Compile / resourceGenerators += Def.task {
      (webpack in (frontend, Compile, frontend / fastOptJS)).value.map(_.data)
    }.taskValue,

    watchSources ++= (frontend / watchSources).value

  )
  .dependsOn(shared.jvm)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings: _*)
  .settings(

    name := "frontend",

    scalaJSUseMainModuleInitializer := true,

    Compile / npmDependencies ++= Seq(
      "react" -> "16.5.1",
      "react-dom" -> "16.5.1",
      //      "normalize.css" -> "8.0.0"
    ),

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0",
      "com.lihaoyi" %%% "upickle" % upickleV,
      "com.github.japgolly.scalajs-react" %%% "core" % "1.7.6",
    )

  ).dependsOn(shared.js)

lazy val shared =
  (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("shared"))
    .settings(commonSettings: _*)
    .settings(
      name := "shared"
    )

def commonSettings = Seq(
  scalaVersion := scalaV,
  scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8", "-Ywarn-dead-code", "-unchecked", "-Xlint"),
  libraryDependencies += "com.lihaoyi" %% "upickle" % upickleV
)