import Dependencies._

enablePlugins(GatlingPlugin)

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "org.load",
//      scalaVersion := "2.13.6",
      scalaVersion := "3.3.0",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "load-generator",
    libraryDependencies ++= gatling
  )

addCommandAlias("r", ";Gatling / testOnly payments.CircuitBreakerSimulation")
addCommandAlias("r2", ";Gatling / testOnly payments.OkLimitSimulation")
addCommandAlias("rl", ";reload")
addCommandAlias("c", ";compile")
