name := "learning-finagle-serverset"

version := "0.1"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "com.twitter" % "finagle-serversets_2.10" % "6.29.0" ,
  "com.twitter.finatra" % "finatra-http_2.10" % "2.0.1" ,
  "com.twitter" % "finagle-httpx_2.10" % "6.29.0" ,
  "org.apache.logging.log4j" % "log4j-core" % "2.4.1")

resolvers += "twitter-repo" at "https://maven.twttr.com"