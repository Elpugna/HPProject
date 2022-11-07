ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.10"

lazy val root = (project in file("."))
  .enablePlugins(Cinnamon)
  .settings(
    name := "hp_project",
    idePackagePrefix := Some("com.applaudostudios.fcastro.hp_project")
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.3",

  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.14" % Test,
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2",
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
  "org.scalatest" %% "scalatest-featurespec" % "3.2.14" % Test
)
libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "3.0.4",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-coordination" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.datastax.oss" % "java-driver-core" % "4.14.1",
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.6"
)

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
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

jacocoExcludes := Seq(
  "actors.JsonLoaderActor.*"
)

jacocoReportSettings := JacocoReportSettings()
  .withThresholds(
    JacocoThresholds(
      instruction = 80,
      method = 100,
      branch = 100,
      complexity = 100,
      line = 90,
      clazz = 100)
  )