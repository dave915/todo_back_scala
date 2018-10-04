name := "de_todo_back"

version := "1.0"

lazy val `de_todo_back` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq( ws , specs2 % Test , guice ,
  "com.typesafe.play" %% "play-slick" % "3.0.3",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
  "mysql" % "mysql-connector-java" % "5.1.42",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.6",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "jp.co.bizreach" %% "play-modules-redis" % "2.6.0",
  "com.typesafe.play" %% "play-logback" % "2.6.19"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

