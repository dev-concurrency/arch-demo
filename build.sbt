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
  )
    .mkString(start = "-Yimports:", sep = ",", end = ""),
)

run / javaOptions ++= {
  javaOptions.value.foreach(println)
  println("javaOptions values")
  val props = sys.props.toList
  props.filter(
    (p: (String, String)) => p._1 == "config.file"
  ).map {
    case (key, value) =>
      val t = s"""-D$key="$value""""
      println(t)
      t
  }
  // List()
}

val AkkaVersion = "2.9.1"
val KafkaVersion = "5.0.0"
val LogbackVersion = "1.4.14"
val ScalaVersion = "3.3.0"
val JacksonVersion = "2.11.4"
val AkkaHttpVersion = "10.6.0"
val akkaManagement = "1.5.0"
val cassandra = "1.2.0"
val akkaPersistenceR2dbc = "1.2.1"
val akkaProjection = "1.5.1"
val izumi = "1.2.5"

lazy val root = project
  .in(file("."))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(autoImportSettings)
  .settings(
    scalaVersion := ScalaVersion,
    // scalafmtOnCompile := true,

    Compile / run / fork := true,
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
    libraryDependencies ++= Seq(
      "com.typesafe.akka"         %% "akka-actor-typed"           % AkkaVersion,
      "com.typesafe.akka"         %% "akka-discovery"             % AkkaVersion,
      "ch.qos.logback"             % "logback-classic"            % LogbackVersion,
      "com.typesafe.akka"         %% "akka-actor-testkit-typed"   % AkkaVersion % Test,
      "com.lihaoyi"               %% "requests"                   % "0.8.0",
      "org.json4s"                %% "json4s-native"              % "4.0.6",
      "com.typesafe.akka"         %% "akka-stream"                % AkkaVersion,
      "com.typesafe.akka"         %% "akka-stream-kafka"          % KafkaVersion,
      "com.fasterxml.jackson.core" % "jackson-databind"           % JacksonVersion,
      "com.typesafe.akka"         %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka"         %% "akka-http"                  % AkkaHttpVersion,
      "io.7mind.izumi"            %% "distage-core"               % izumi,
      "io.7mind.izumi"            %% "distage-extension-config"   % izumi,
      // "io.confluent" % "kafka-avro-serializer" % "7.5.2",
      "com.typesafe.akka"         %% "akka-slf4j"                 % AkkaVersion,
      // "ch.qos.logback" % "logback-classic" % "1.2.3"

      "com.typesafe.akka"             %% "akka-cluster-typed"                % AkkaVersion,
      "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % AkkaVersion,
      "com.typesafe.akka"             %% "akka-slf4j"                        % AkkaVersion,
      "com.typesafe.akka"             %% "akka-serialization-jackson"        % AkkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-typed"            % AkkaVersion,
      "com.typesafe.akka"             %% "akka-stream"                       % AkkaVersion,
      "com.typesafe.akka"             %% "akka-discovery"                    % AkkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-cassandra"        % cassandra,
      "com.lightbend.akka"            %% "akka-persistence-r2dbc"            % akkaPersistenceR2dbc,
      "com.lightbend.akka"            %% "akka-projection-r2dbc"             % akkaProjection,
      "com.lightbend.akka"            %% "akka-projection-core"              % akkaProjection,
      "com.lightbend.akka"            %% "akka-projection-eventsourced"      % akkaProjection,
      "com.typesafe.akka"             %% "akka-slf4j"                        % AkkaVersion,
      "com.typesafe.akka"             %% "akka-serialization-jackson"        % AkkaVersion,
      "com.typesafe.akka"             %% "akka-persistence-typed"            % AkkaVersion,
      "com.typesafe.akka"             %% "akka-stream"                       % AkkaVersion,
      "com.typesafe.akka"             %% "akka-discovery"                    % AkkaVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagement,
      "com.lightbend.akka.management" %% "akka-management-cluster-http"      % akkaManagement,
      "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % akkaManagement,
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
  )

ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven")
ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/")

val scenario1 = Seq(
  "import demo.examples.*",
  "import demo.examples.ServerMain.*",
  "init",
)

val scenario2 = Seq(
  "import event_sourcing.examples.WalletOperations.*",
  "init",
)

val scenario3 = Seq(
  "import components.examples.*",
)

lazy val selectedScenario = sys.env.get("SCENARIO").getOrElse("scenario1")
lazy val scenarioInititalCommands = scenarios(selectedScenario)
lazy val scenarios = Map(
  "scenario1" -> scenario1,
  "scenario2" -> scenario2,
  "scenario3" -> scenario3,
)

ThisBuild / scalacOptions ++=
  Seq(
    "-explain",
    "-deprecation",
    // "-Wunused:all",
    "-Yretain-trees",
    // "-Yexplicit-nulls",
    // "-Ysafe-init",
  )
// ) ++ Seq("-new-syntax", "-rewrite")
// ) ++ Seq("-rewrite", "-indent")

Compile / console / initialCommands += scenarioInititalCommands.mkString(";\n")

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger

addCommandAlias("c", "compile")
addCommandAlias("t", "test")
addCommandAlias("styleCheck", "scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("styleFix", "scalafmtSbt; scalafmtAll")
addCommandAlias("rl", "reload plugins; update; reload return")

selectedScenario match{
  case "scenario3" => 
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.examples.run")
      .value
  case _ =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" demo.examples.run")
      .value
}