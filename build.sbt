// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbt.Def
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion = "2.5.16"
lazy val scalaV = "2.12.6"
lazy val upickleV = "0.6.6"

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
      "de.heikoseeberger" %% "akka-http-upickle" % "1.21.1",
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),

    //TODO: understand what this means
    resourceGenerators in Compile += Def.task {
      (webpack in(frontend, Compile, fastOptJS in frontend)).value.map(_.data)
    }.taskValue,

    watchSources ++= (watchSources in frontend).value

  )
  .dependsOn(shared.jvm)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings: _*)
  .settings(

    name := "frontend",

    scalaJSUseMainModuleInitializer := true,

    npmDependencies in Compile ++= Seq(
      "react" -> "16.5.1",
      "react-dom" -> "16.5.1"),

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      "com.lihaoyi" %%% "upickle" % upickleV,
      "com.github.japgolly.scalajs-react" %%% "core" % "1.3.0",
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
  scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8", "-Ywarn-dead-code", "-unchecked", "-Xlint", "-Ywarn-unused-import"),
  libraryDependencies += "com.lihaoyi" %% "upickle" % upickleV
)