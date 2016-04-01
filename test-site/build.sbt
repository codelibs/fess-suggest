name := """fess-suggest-test"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += Resolver.url("codelibs", url("http://maven.codelibs.org/"))(Resolver.ivyStylePatterns)

resolvers += (
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
  )

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)

libraryDependencies += "org.codelibs" % "elasticsearch-cluster-runner" % "2.1.2.0"

libraryDependencies += "org.codelibs" % "elasticsearch-fess-suggest" % "2.1.2"

libraryDependencies += "org.codelibs" % "elasticsearch-analysis-kuromoji-neologd" % "2.1.1"

libraryDependencies += "org.codelibs.fess" % "fess-suggest" % "2.1.4-SNAPSHOT"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.1"

libraryDependencies += "org.codehaus.groovy" % "groovy-all" % "2.4.0"

libraryDependencies += "log4j" % "log4j" % "1.2.17"
