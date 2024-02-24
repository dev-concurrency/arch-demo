resolvers += "Akka library repository".at("https://repo.akka.io/maven")

addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.4.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.15"
