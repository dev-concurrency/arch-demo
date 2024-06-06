import PluginsDependencies.V

//resolvers += "Akka library repository".at("https://repo.akka.io/maven")
//addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.4.3")

addSbtPlugin("org.typelevel" % "sbt-fs2-grpc" % V.sbt_fs2_grpc)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % V.sbt_scalafix)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % V.sbt_scalafmt)

addSbtPlugin("com.thesamet"                    % "sbt-protoc"     % V.sbt_protoc)
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % V.scalapbCompiler

addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % V.smithy4s_sbt_codegen)

addSbtPlugin("com.github.sbt" % "sbt-avro" % V.sbt_avro)

// Java sources compiled with one version of Avro might be incompatible with a
// different version of the Avro library. Therefore we specify the compiler
// version here explicitly.
libraryDependencies += "org.apache.avro" % "avro-compiler" % V.avro_compiler
