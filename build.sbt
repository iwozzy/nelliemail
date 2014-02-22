import play.Project._

name := """hello-play-java"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
	"org.webjars" %% "webjars-play" % "2.2.0", 
	"org.webjars" % "bootstrap" % "2.3.1",
  "com.evernote" % "evernote-api" % "1.25.1",
  "com.google.api-client" % "google-api-client" % "1.16.0-rc",
  "org.codehaus.jackson" % "jackson-asl" % "0.9.5",
  "javax.mail" % "mail" % "1.4.7",
  javaJdbc,
  javaEbean)

playJavaSettings
