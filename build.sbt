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

// format: off
val V = new {
  val distage       = "1.2.3"
  val logstage      = distage
  val scalatest     = "3.2.18"
  val scalacheck    = "1.17.0"
  val http4s        = "0.23.26"
  val doobie        = "1.0.0-RC5"
  val catsCore      = "2.10.0"
  val zio           = "2.0.21"
  val zioCats       = "23.0.0.8"
  val circeGeneric  = "0.14.6"

  val scalaVersion = "3.3.3"
  val akkaVersion = "2.9.2"
  val kafkaVersion = "5.0.0"
  val logbackVersion = "1.4.14"
  val jacksonVersion = "2.11.4"
  val akkaHttpVersion = "10.6.1"
  val akkaManagement = "1.5.1"
  val cassandra = "1.2.0"
  val akkaPersistenceR2dbc = "1.2.3"
  val akkaProjection = "1.5.3"
  val cats = "2.10.0"
  val catsEffect = "3.5.4"
  val fs2 = "3.10.0"
  val iron = "2.5.0"

  val grpc                 = "1.56.0"
  val scalapbCommonProtos  = "2.9.6-0"
  val avroCompiler         = "1.11.3"
  
  val chimney              = "0.8.5"
}

val Deps = new {
      val logbackClassic =             "ch.qos.logback"                % "logback-classic"                    % V.logbackVersion
      val requests =                   "com.lihaoyi"                   %% "requests"                          % "0.8.0"
      val json4sNative =               "org.json4s"                    %% "json4s-native"                     % "4.0.6"
      val distageCore =                "io.7mind.izumi"                %% "distage-core"                      % V.distage
      val distageConfig =              "io.7mind.izumi"                %% "distage-extension-config"          % V.distage 
      val distagePlugins =             "io.7mind.izumi"                %% "distage-extension-plugins"         % V.distage

      // "io.confluent" % "kafka-avro-serializer" % "7.5.2",
      val akkaSlf4j =                  "com.typesafe.akka"             %% "akka-slf4j"                        % V.akkaVersion
      // "ch.qos.logback" % "logback-classic" % "1.2.3"
      val akkaActorTyped =             "com.typesafe.akka"             %% "akka-actor-typed"                  % V.akkaVersion
      val akkaDiscovery =              "com.typesafe.akka"             %% "akka-discovery"                    % V.akkaVersion
      val akkaTestkitTyped =           "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % V.akkaVersion % Test
      val akkaStream =                 "com.typesafe.akka"             %% "akka-stream"                       % V.akkaVersion
      val akkaStreamKafka =            "com.typesafe.akka"             %% "akka-stream-kafka"                 % V.kafkaVersion
      val jacksonDatabind =            "com.fasterxml.jackson.core"    % "jackson-databind"                   % V.jacksonVersion
      val akkaSerializationJackson =   "com.typesafe.akka"             %% "akka-serialization-jackson"        % V.akkaVersion
      val akkaHttp =                   "com.typesafe.akka"             %% "akka-http"                         % V.akkaHttpVersion
      val akkaClusterTyped =           "com.typesafe.akka"             %% "akka-cluster-typed"                % V.akkaVersion
      val akkaClusterSharding =        "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % V.akkaVersion
      val akkaClusterBootstrap =       "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % V.akkaManagement
      val akkaClusterHttp =            "com.lightbend.akka.management" %% "akka-management-cluster-http"      % V.akkaManagement
      val akkaPersistence =            "com.typesafe.akka"             %% "akka-persistence-typed"            % V.akkaVersion
      val akkaPersistenceCassandra =   "com.typesafe.akka"             %% "akka-persistence-cassandra"        % V.cassandra
      val akkaPersistenceR2dbc =       "com.lightbend.akka"            %% "akka-persistence-r2dbc"            % V.akkaPersistenceR2dbc
      val akkaProjectionR2dbc =        "com.lightbend.akka"            %% "akka-projection-r2dbc"             % V.akkaProjection
      val akkaProjectionCore =         "com.lightbend.akka"            %% "akka-projection-core"              % V.akkaProjection
      val akkaProjectionEventsourced = "com.lightbend.akka"            %% "akka-projection-eventsourced"      % V.akkaProjection
      val akkaKubenetes =              "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % V.akkaManagement

      val cats =                       "org.typelevel"                 %% "cats-core"                         % V.cats
      val catsEffect =                 "org.typelevel"                 %% "cats-effect"                       % V.catsEffect

      val catsMtl =                    "org.typelevel"                 %% "cats-mtl"                          % "1.4.0"

      val fs2 =                        "co.fs2"                        %% "fs2-core"                          % V.fs2
      val fs2Io =                      "co.fs2"                        %% "fs2-io"                            % V.fs2
      
      val iron =                       "io.github.iltotore"            %% "iron"                              % V.iron
      val ironCirce =                  "io.github.iltotore"            %% "iron-circe"                        % V.iron
      val ironCats =                   "io.github.iltotore"            %% "iron-cats"                         % V.iron
      val ironDecline =                "io.github.iltotore"            %% "iron-decline"                      % V.iron
      
      val grpcNettyShaded            = "io.grpc"                        %  "grpc-netty-shaded"                % scalapb.compiler.Version.grpcJavaVersion
      val grpc                       = "io.grpc"                        %  "grpc-services"                    % V.grpc
      val scalapbCommonProtos        = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % V.scalapbCommonProtos % "protobuf"
      
      
    val chimney                    = "io.scalaland"       %% "chimney"                        % V.chimney
    val chimneyProtobufs           = "io.scalaland"       %% "chimney-protobufs"              % V.chimney
    val chimneyJavaCollections     = "io.scalaland"       %% "chimney-java-collections"       % V.chimney
}

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

lazy val root = project
  .in(file("."))
  .enablePlugins(Fs2Grpc)
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(autoImportSettings)
  .settings(
    scalaVersion := V.scalaVersion,
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
    
    // Compile / PB.targets := Seq(
    //     PB.gens.java -> (Compile / sourceManaged).value,
    //     scalapb.gen() -> (Compile / sourceManaged).value
    //  ),
    // scalapbCodeGeneratorOptions += CodeGeneratorOption.Fs2Grpc,
    
    libraryDependencies ++= Seq(
    
      Deps.grpc,
      Deps.grpcNettyShaded,
      Deps.scalapbCommonProtos,

      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.26",

      Deps.chimney,
      Deps.chimneyProtobufs,
      Deps.chimneyJavaCollections,

      Deps.akkaActorTyped,
      Deps.akkaSlf4j,
      Deps.akkaStream,
      Deps.akkaStreamKafka,
      Deps.akkaSerializationJackson,
      Deps.akkaHttp,
      Deps.akkaClusterTyped,
      Deps.akkaClusterSharding,
      Deps.akkaClusterBootstrap,
      Deps.akkaClusterHttp,
      Deps.akkaPersistence,
      Deps.akkaPersistenceCassandra,
      Deps.akkaPersistenceR2dbc,
      Deps.akkaProjectionR2dbc,
      Deps.akkaProjectionCore,
      Deps.akkaProjectionEventsourced,

      "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % "2.4.1",

      Deps.cats,
      Deps.catsEffect,
      Deps.catsMtl,
      Deps.fs2,
      Deps.logbackClassic,
      Deps.requests,
      Deps.json4sNative,
      Deps.distageCore,
      Deps.distageConfig,
      Deps.distagePlugins,
      Deps.akkaKubenetes,
      Deps.iron,
      Deps.ironCirce,
      Deps.ironCats,
      Deps.ironDecline,
    ),
    // akkaGrpcCodeGeneratorSettings += "server_power_apis",
  )

// format: on

ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven")
ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/")

val scenario1 = Seq(
  "import demo.examples.*",
  "init",
)

val scenario2 = Seq(
  "import demo.examples.*",
  "import demo.examples.ServerMain.*",
  "init",
)

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

lazy val selectedScenario = sys.env.get("SCENARIO").getOrElse("scenario1")
lazy val scenarioInititalCommands = scenarios(selectedScenario)

lazy val scenarios = Map(
  "scenario1" -> scenario1,
  "scenario2" -> scenario2,
  "scenario3" -> scenario3,
  "scenario4" -> scenario4,
  "scenario5" -> scenario5,
  "scenario6" -> scenario6,
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
// ) ++ Seq("-rewrite", "-source", "3.4-migration")

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger

addCommandAlias("c", "compile")
addCommandAlias("t", "test")
addCommandAlias("styleCheck", "scalafmtSbtCheck; scalafmtCheckAll")
addCommandAlias("styleFix", "scalafmtSbt; scalafmtAll")
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

  case _ =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" demo.examples.run")
      .value
}
