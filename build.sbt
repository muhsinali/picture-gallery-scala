name := """picture-gallery"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

