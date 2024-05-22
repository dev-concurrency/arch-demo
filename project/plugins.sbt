//resolvers += "Akka library repository".at("https://repo.akka.io/maven")
//addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.4.1")

addSbtPlugin("org.typelevel" % "sbt-fs2-grpc" % "2.7.14")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.12.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("com.thesamet"                    % "sbt-protoc"     % "1.0.7")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.15"

addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.19")

addSbtPlugin("com.github.sbt" % "sbt-avro" % "3.4.3")

// Java sources compiled with one version of Avro might be incompatible with a
// different version of the Avro library. Therefore we specify the compiler
// version here explicitly.
libraryDependencies += "org.apache.avro" % "avro-compiler" % "1.11.3"
