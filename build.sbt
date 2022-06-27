scalaVersion := "2.13.8"

val akkaVersion            = "2.6.19"
lazy val leveldbVersion    = "0.12"
lazy val leveldbjniVersion = "1.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-slf4j"       % akkaVersion,
  "org.iq80.leveldb"          % "leveldb"          % leveldbVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all"   % leveldbjniVersion,
  "com.typesafe.akka"        %% "akka-persistence" % akkaVersion,
  "ch.qos.logback"            % "logback-classic"  % "1.2.11",
  "org.scalatest"            %% "scalatest"        % "3.2.12" % Test
)

scalafmtOnCompile := true
fork              := true
