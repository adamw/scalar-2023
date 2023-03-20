import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.scalar",
  scalaVersion := "3.2.2"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15" % Test

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "scalar-2023")
  .aggregate(tapirs, oxes, sttps)

lazy val tapirs: Project = (project in file("tapirs"))
  .settings(commonSettings: _*)
  .settings(
    name := "tapirs",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-id" % "0.1.1",
      "com.softwaremill.sttp.tapir" %% "tapir-json-upickle" % "1.2.4",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.2.4",
      scalaTest
    )
  )

lazy val oxes: Project = (project in file("oxes"))
  .settings(commonSettings: _*)
  .settings(
    name := "oxes",
    libraryDependencies ++= Seq(
      "com.softwaremill.ox" %% "core" % "0.0.4",
      "ch.qos.logback" % "logback-classic" % "1.4.5",
      scalaTest
    ),
    javaOptions += "--enable-preview --add-modules jdk.incubator.concurrent"
  )

lazy val sttps: Project = (project in file("sttps"))
  .settings(commonSettings: _*)
  .settings(
    name := "sttps",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M1",
      scalaTest
    )
  )
