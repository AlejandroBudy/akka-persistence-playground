scalaVersion := "2.13.8"

lazy val akkaVersion       = "2.6.9"
lazy val leveldbVersion    = "0.12"
lazy val leveldbjniVersion = "1.8"
lazy val postgresVersion   = "42.3.6"
lazy val cassandraVersion  = "1.0.5"

libraryDependencies ++= Seq(
  "org.iq80.leveldb"          % "leveldb"          % leveldbVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all"   % leveldbjniVersion,
  "com.typesafe.akka"        %% "akka-persistence" % akkaVersion,

  // JDBC with PostgreSQL
//  "org.postgresql"       % "postgresql"            % postgresVersion,
  // "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.3",
  // Cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra"          % cassandraVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandraVersion % Test
)

scalafmtOnCompile := true
fork              := true
