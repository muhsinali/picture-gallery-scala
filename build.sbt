name := """picture-gallery"""
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  filters,
  jdbc,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.105",
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.7"  // image processing & manipulation library
)

