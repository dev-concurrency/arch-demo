import sbt._

object Dependencies {

// format: off
  object V {
    val scalaLTSVersion      = "3.3.3"
    // val scalaLTSVersion      = "3.4.2"
    val scalaLatestVersion   = "3.4.2"
    val distage              = "1.2.8"
    val logstage             = distage
    val scalatest            = "3.2.18"
    val scalacheck           = "1.17.0"
    val catsCore             = "2.10.0"
    val zio                  = "2.0.21"
    val zioCats              = "23.0.0.8"
    val circeGeneric         = "0.14.6"
    val akkaVersion          = "2.9.3"
    val akkaGrpc             = "2.4.3"
    val kafkaVersion         = "6.0.0"
    val logbackVersion       = "1.4.14"
    val jacksonVersion       = "2.11.4"
    val akkaHttpVersion      = "10.6.3"
    val akkaManagement       = "1.5.2"
    val cassandra            = "1.2.1"
    val akkaPersistenceR2dbc = "1.2.4"
    val akkaProjection       = "1.5.4"
    val cats                 = "2.10.0"
    val catsEffect           = "3.5.4"
    val fs2                  = "3.10.2"
    val iron                 = "2.5.0"
    val grpc                 = "1.64.0"
    val scalapbCommonProtos  = "2.9.6-0"
    val avroCompiler         = "1.11.3"
    val chimney              = "1.0.0"
    val doobie               = "1.0.0-RC5"
    val skunk                = "1.1.0-M3"
    val postgress            = "42.7.3"
    val commonsCompress      = "1.26.1"
    // https://packages.confluent.io/maven/io/confluent/kafka-avro-serializer/
    val kafkaAvroSerializer  = "7.6.1"
    val smithytranslateTraitsVersion = "0.5.3"
    val http4s                       = "0.23.27"
    val scalapb                      = "0.11.15"
    val avroCompilerVersion          = "1.11.3"
    val fs2Kafka                     = "3.5.1"

  }

  object Deps {
    // val doobieRefined     = "org.tpolecat" %% "doobie-refined" % V.doobie
    val commonsCompress            = "org.apache.commons" % "commons-compress"  % V.commonsCompress 
    val postgresql                 = "org.postgresql" %  "postgresql"           % V.postgress
    val doobieCore                 = "org.tpolecat"  %% "doobie-core"           % V.doobie
    val doobieHikari               = "org.tpolecat"  %% "doobie-hikari"         % V.doobie
    val doobiePostgres             = "org.tpolecat"  %% "doobie-postgres"       % V.doobie
    val doobiePostgresCirce        = "org.tpolecat"  %% "doobie-postgres-circe" % V.doobie
    val doobieScalatest            = "org.tpolecat"  %% "doobie-scalatest"      % V.doobie % Test
    val doobieMunit                = "org.tpolecat"  %% "doobie-munit"          % V.doobie % Test
    val doobieFree                 = "org.tpolecat"  %% "doobie-free"           % V.doobie

    val skunkRefined               = "org.tpolecat"  %% "skunk-refined" % V.skunk
    val skunkPostgis               = "org.tpolecat"  %% "skunk-postgis" % V.skunk
    val skunkDocs                  = "org.tpolecat"  %% "skunk-docs"    % V.skunk
    val skunkCirce                 = "org.tpolecat"  %% "skunk-circe"   % V.skunk
    val skunkCore                  = "org.tpolecat"  %% "skunk-core"    % V.skunk

    val logbackClassic             = "ch.qos.logback"  % "logback-classic"                   % V.logbackVersion
    val requests                   = "com.lihaoyi"    %% "requests"                          % "0.8.0"
    val json4sNative               = "org.json4s"     %% "json4s-native"                     % "4.0.6"
    
    val distageCore                = "io.7mind.izumi" %% "distage-core"                      % V.distage
    val distageConfig              = "io.7mind.izumi" %% "distage-extension-config"          % V.distage 
    val distagePlugins             = "io.7mind.izumi" %% "distage-extension-plugins"         % V.distage

    val kafkaAvroSerializer        = "io.confluent"    % "kafka-avro-serializer"             % V.kafkaAvroSerializer

    val akkaSlf4j                  = "com.typesafe.akka"             %% "akka-slf4j"                        % V.akkaVersion
    // "ch.qos.logback" % "logback-classic" % "1.2.3"

    val akkaActorTyped             = "com.typesafe.akka"             %% "akka-actor-typed"                  % V.akkaVersion
    val akkaDiscovery              = "com.typesafe.akka"             %% "akka-discovery"                    % V.akkaVersion
    val akkaTestkitTyped           = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % V.akkaVersion % Test
    val akkaStream                 = "com.typesafe.akka"             %% "akka-stream"                       % V.akkaVersion
    val akkaStreamKafka            = "com.typesafe.akka"             %% "akka-stream-kafka"                 % V.kafkaVersion
    val jacksonDatabind            = "com.fasterxml.jackson.core"     % "jackson-databind"                  % V.jacksonVersion
    val akkaSerializationJackson   = "com.typesafe.akka"             %% "akka-serialization-jackson"        % V.akkaVersion
    val akkaHttp                   = "com.typesafe.akka"             %% "akka-http"                         % V.akkaHttpVersion
    val akkaClusterTyped           = "com.typesafe.akka"             %% "akka-cluster-typed"                % V.akkaVersion
    val akkaClusterSharding        = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % V.akkaVersion
    val akkaClusterBootstrap       = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % V.akkaManagement
    val akkaClusterHttp            = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % V.akkaManagement
    val akkaPersistence            = "com.typesafe.akka"             %% "akka-persistence-typed"            % V.akkaVersion
    val akkaPersistenceCassandra   = "com.typesafe.akka"             %% "akka-persistence-cassandra"        % V.cassandra
    val akkaPersistenceR2dbc       = "com.lightbend.akka"            %% "akka-persistence-r2dbc"            % V.akkaPersistenceR2dbc
    val akkaProjectionR2dbc        = "com.lightbend.akka"            %% "akka-projection-r2dbc"             % V.akkaProjection
    val akkaProjectionCore         = "com.lightbend.akka"            %% "akka-projection-core"              % V.akkaProjection
    val akkaProjectionEventsourced = "com.lightbend.akka"            %% "akka-projection-eventsourced"      % V.akkaProjection
    val akkaKubernetes             = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % V.akkaManagement
    val akkaGrpc                   = "com.lightbend.akka.grpc"       %% "akka-grpc-runtime"                 % V.akkaGrpc

    val cats                       = "org.typelevel"                 %% "cats-core"                         % V.cats
    val catsEffect                 = "org.typelevel"                 %% "cats-effect"                       % V.catsEffect
    val catsMtl                    = "org.typelevel"                 %% "cats-mtl"                          % "1.4.0"

    val fs2                        = "co.fs2"                        %% "fs2-core"                          % V.fs2
    val fs2Io                      = "co.fs2"                        %% "fs2-io"                            % V.fs2
    
    val iron                       = "io.github.iltotore"            %% "iron"                              % V.iron
    val ironCirce                  = "io.github.iltotore"            %% "iron-circe"                        % V.iron
    val ironCats                   = "io.github.iltotore"            %% "iron-cats"                         % V.iron
    val ironDecline                = "io.github.iltotore"            %% "iron-decline"                      % V.iron
    
    val grpcNettyShaded            = "io.grpc"                             %  "grpc-netty-shaded"                      % scalapb.compiler.Version.grpcJavaVersion
    val grpc                       = "io.grpc"                             %  "grpc-services"                          % V.grpc
    val scalapbCommonProtos        = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % V.scalapbCommonProtos % "protobuf"

    val scalapbRuntime               = "com.thesamet.scalapb" %% "scalapb-runtime"                            % V.scalapb
    val scalapbProtobufu             = "com.thesamet.scalapb" %% "scalapb-runtime"                            % V.scalapb % "protobuf"
    
    val chimney                    = "io.scalaland"       %% "chimney"                        % V.chimney
    val chimneyProtobufs           = "io.scalaland"       %% "chimney-protobufs"              % V.chimney
    val chimneyJavaCollections     = "io.scalaland"       %% "chimney-java-collections"       % V.chimney

    val http4s                     = "org.http4s"         %% "http4s-ember-server"            % V.http4s
    
    val avro                       = "org.apache.avro"     % "avro"                           % V.avroCompilerVersion
    
    val fs2Kafka                   = "com.github.fd4s"    %% "fs2-kafka"                      % V.fs2Kafka
    val munit                      = "org.scalameta"      %% "munit"                          % "1.0.0" % Test
    val catsMunit                  = "org.typelevel"      %% "munit-cats-effect"              % "2.0.0" % Test


  }

// format: on

  val HybridDeps = Seq(
      Deps.commonsCompress,

      Deps.postgresql,

      Deps.doobiePostgresCirce,
      Deps.doobieHikari,
      Deps.doobiePostgres,
      Deps.doobieCore,
      Deps.doobieScalatest,
      Deps.doobieMunit,
      Deps.doobieFree,
    
      Deps.grpc,
      Deps.grpcNettyShaded,
      Deps.scalapbCommonProtos,

      Deps.http4s,

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
      Deps.akkaGrpc,

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
      Deps.akkaKubernetes,
      Deps.iron,
      Deps.ironCirce,
      Deps.ironCats,
      Deps.ironDecline,
      
      Deps.avro,
      Deps.fs2Kafka,
      Deps.kafkaAvroSerializer,

      Deps.munit,
      Deps.catsMunit,

  )

}
