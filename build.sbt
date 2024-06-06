import Dependencies.V
import Dependencies.Deps
import Dependencies.HybridDeps

// format: off

lazy val autoImportSettings = Seq(
  scalacOptions += Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "scala.util.chaining",
    "akka.actor.typed",
    "scala.concurrent",
    "akka.actor.typed.scaladsl",
    "akka.actor.typed.scaladsl.AskPattern",
    "scala.concurrent.duration",
    "akka.util",
    "scala.util",

    // "cats.implicits",
    // "cats",
    // "cats.effect",
  )
    .mkString(start = "-Yimports:", sep = ",", end = ""),
)

lazy val commonSettings = Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty,
  scalaVersion := V.scalaLTSVersion,
  organization := "org",
  organizationName := "Demos",
  semanticdbEnabled := true, // enable SemanticDB
  ThisBuild / evictionErrorLevel := Level.Info,
  dependencyOverrides ++= Seq(
  ),
  ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
  ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/"),
)

lazy val appSettings = Seq(
  scalaVersion := V.scalaLatestVersion,
  dependencyOverrides ++= Seq(
  ),
  scalacOptions ++=
    Seq(
      "-explain",
      "-Ysafe-init",
      "-deprecation",
      "-feature",
      "-Yretain-trees",
      "-Xmax-inlines",
      "50",
      // "-Yexplicit-nulls",
      // "-Wunused:all",
    )
  // ) ++ Seq("-new-syntax", "-rewrite")
  // ) ++ Seq("-rewrite", "-indent")
  // ) ++ Seq("-rewrite", "-source", "3.4-migration")
)

def mapGen(name: String) = {
  import scala.collection.mutable.HashMap
  
  val m = new HashMap[String, String]
  sys.env.get("VARIABLES") match{
    case Some(variables) => variables.split(",").foreach { v =>
      val res = (v -> sys.env.get(s"${name.toUpperCase()}_$v").get)
      println(s"Adding env-var: '${res._1}' with value '${res._2}'")
      m += res
    }
    case None => 
  }
  m.toMap
}


lazy val restApi = project
  .in(file("modules/rest-api"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % smithy4sVersion.value,
      "com.disneystreaming.smithy"   %  "smithytranslate-traits"  % V.smithytranslateTraitsVersion,
    ),
    // Compile / smithy4sOutputDir := (Compile / baseDirectory).value / "src/main/scala/smithy",
  )

lazy val grpcApi = project
  .in(file("modules/grpc-api"))
  .enablePlugins(Fs2Grpc)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.grpc,
      Deps.scalapbCommonProtos,
      Deps.scalapbProtobufu,
    ),
    PB.protocVersion := "4.27.0",
    // fs2GrpcOutputPath := (Compile / baseDirectory).value / "src/main/scala/fs2-grpc",
    // scalapbProtobufDirectory := (Compile / baseDirectory).value / "src/main/scala/scalapb",
  )


lazy val avroApi = project
  .in(file("modules/avro-api"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    scalaVersion := V.scalaLTSVersion,
    libraryDependencies ++= Seq(
      Deps.avro,
    ),
  )

lazy val root = project
  .in(file("."))
  .settings(autoImportSettings)
  .settings(commonSettings)
  .settings(appSettings)
  .settings(
    scalaVersion := V.scalaLatestVersion,
    // scalafmtOnCompile := true,
    Compile / run / fork := true,
    Compile / console / initialCommands += scenarioInititalCommands.mkString(";\n"),
    mainClass := Some("demo.examples.run"),
    // Compile / discoveredMainClasses := Seq(),
    Compile / run / javaOptions ++= {
      val props = sys.props.toList
      props.filter(
        (p: (String, String)) => p._1 == "config.file"
      ).map {
        case (key, value) => s"""-D$key="$value""""
      }
    },
    libraryDependencies ++= HybridDeps,
  )
  // .aggregate(restApi)
  // .aggregate(grpcApi)
  // .aggregate(avroApi)
  .dependsOn(restApi)
  .dependsOn(grpcApi)
  .dependsOn(avroApi)
  .dependsOn(journal_events_akka_event_sourced)
  // .aggregate(journal_events_akka_event_sourced)

// format: on

lazy val base_akka_event_sourced = project
  .in(file("modules/event-sourced/base"))
  .settings(commonSettings)

lazy val journal_events_akka_event_sourced = project
  .in(file("modules/event-sourced/events"))
  .settings(commonSettings)
  .dependsOn(base_akka_event_sourced)
  // .aggregate(base_akka_event_sourced)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    // https://repo1.maven.org/maven2/com/google/protobuf/protoc/
    PB.protocVersion := "4.27.0",
    libraryDependencies += Deps.scalapbProtobufu,
  )

val scenario1 = Seq(
  "import demo.examples.*",
  "init",
)

val scenario2 = Seq(
  "import demo.examples.*",
  "import demo.examples.ServerMain.*",
  "init",
)

// complete setup
val scenario3 = Seq(
  "import event_sourcing.examples.WalletOperations.*",
  "init",
)

val scenario4 = Seq(
  "import components.examples.*",
)

val scenario5 = Seq(
  "import components.examples.plugins.*",
)

val scenario6 = Seq(
)

val scenario7 = Seq(
)

val scenario8 = Seq(
  "import components.infrastructure.cluster.WalletOperations.*",
  "init",
)

lazy val selectedScenario = sys.env.get("SCENARIO").getOrElse("scenario1")
lazy val scenarioInititalCommands = scenarios(selectedScenario)

lazy val scenarios = Map(
  "scenario1" -> scenario1,
  "scenario2" -> scenario2,
  "scenario3" -> scenario3,
  "scenario4" -> scenario4,
  "scenario5" -> scenario5,
  "scenario6" -> scenario6,
  "scenario7" -> scenario7,
  "scenario8" -> scenario8,
)

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger

addCommandAlias("ll", "projects")
addCommandAlias("cd", "project")
addCommandAlias("c", "compile")
addCommandAlias("cc", "clean; compile")
addCommandAlias("t", "test")
addCommandAlias("styleCheck", "scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("styleFix", "scalafmtSbt; scalafmtAll; scalafix RemoveUnused; scalafix OrganizeImports")
addCommandAlias("styleF", "scalafmtSbt; scalafmtAll")
addCommandAlias("rl", "reload plugins; update; reload return")

selectedScenario match {

  case "scenario4" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.examples.run")
      .value

  case "scenario5" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.examples.plugins.run")
      .value

  case "scenario6" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.main.run")
      .value

  case "scenario7" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" infrastructure.components.persistence.run")
      .value

  case "scenario8" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.infrastructure.cluster.run")
      .value

  case _ =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" demo.examples.run")
      .value

}
