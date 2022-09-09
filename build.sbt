ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"


lazy val root = (project in file(".")).enablePlugins(Cinnamon)
  .settings(
    name := "HPProject",
    idePackagePrefix := Some("com.applaudostudios.fcastro.HPProject")
  )


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"           % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream"                % AkkaVersion,
  "com.typesafe.akka" %% "akka-http"                  % AkkaHttpVersion,
  "ch.qos.logback"    % "logback-classic"             % "1.2.11"
)
libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "3.0.4",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion
)

libraryDependencies ++= Seq(
   "com.typesafe.akka" %% "akka-coordination"         % AkkaVersion,
   "com.typesafe.akka" %% "akka-cluster"              % AkkaVersion,
   "com.typesafe.akka" %% "akka-cluster-tools"        % AkkaVersion
)

libraryDependencies ++=Seq(
  "com.typesafe.akka" %% "akka-discovery"             % AkkaVersion,
  "com.datastax.oss"  %  "java-driver-core"           % "4.14.1",
  "com.typesafe.akka" %% "akka-persistence-typed"     % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.6"
)

libraryDependencies ++= Seq(
  "io.spray" %%  "spray-json"                         % "1.3.6",
  "com.typesafe.akka" %% "akka-http-spray-json"       % AkkaHttpVersion
)

libraryDependencies ++= Seq(
  Cinnamon.library.cinnamonAkka,
  Cinnamon.library.cinnamonPrometheus,
  Cinnamon.library.cinnamonPrometheusHttpServer,
  Cinnamon.library.cinnamonCHMetrics,
  Cinnamon.library.cinnamonAkkaTyped,
  Cinnamon.library.cinnamonAkkaPersistence,
  Cinnamon.library.cinnamonAkkaStream,
  Cinnamon.library.cinnamonAkkaProjection,
  Cinnamon.library.cinnamonAkkaHttp,
  Cinnamon.library.cinnamonAkkaGrpc
)

run / cinnamon := true
test / cinnamon := true
cinnamonLogLevel := "INFO"