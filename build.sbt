organization := "com.transparencyrights.demo"
name := "wcplus"
version := "0.0-SNAPSHOT"

scalaVersion := "2.11.7"
scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")


val scaldingVersion = "0.13.1-cch-ffc2"
val cascadingVersion = "3.0.1"
val cascadingVersionHbase = "3.0.0"

val hadoopVersion = "2.6.0"
val apacheTezVersion = "0.6.1"

val cascadingFabric = sys.props.getOrElse("CASCADING_FABRIC", "hadoop2-tez") // can be "hadoop", "hadoop2-mr1" or "hadoop2-tez"

resolvers += Resolver.mavenLocal


libraryDependencies ++= Seq(
  "aaaaa.goddamnit" %% "guava-fix" % "1.0.2",
  "aaaaa.goddamnit" %% "guava-fix" % "1.0.2" % "test",
  "com.google.guava" % "guava" % "18.0"
)

libraryDependencies ++= Seq(

  "org.apache.hbase" % "hbase-server" % "0.98.12-hadoop2",
    
  // "commons-digester" % "commons-digester" % "1.8" exclude("commons-beanutils", "commons-beanutils")
  "commons-configuration" % "commons-configuration" % "1.10"
)


libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0"
)

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion
      exclude("com.sun.jersey", "jersey-server"), // wtf ?

  //exclude("commons-configuration", "commons-configuration") // manually required at version 1.10

  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVersion
    exclude("stax", "stax-api") // now "javax.xml.stream" % "stax-api"
    exclude("com.sun.jersey", "jersey-server") // wtf ?
    exclude("com.sun.jersey", "jersey-json")
    exclude("asm", "asm")
    exclude("tomcat", "jasper-runtime") // JSPs already provided by Jetty, also included here
    exclude("tomcat", "jasper-compiler"), // JSPs already provided by Jetty, also included here
  // we actually use neither, we just need hadoop.conf.Configuration

  "org.ow2.asm" % "asm" % "4.0" // because we rejected the older version
)



libraryDependencies ++= Seq(
  /*
	"com.twitter" %% "scalding" % scaldingVersion
      exclude("cascading", "cascading-core")
      exclude("cascading", "cascading-hadoop")
      exclude("cascading", "cascading-local"), */
  "com.twitter" %% "scalding-core" % scaldingVersion
    exclude("cascading", "cascading-core")
    exclude("cascading", "cascading-hadoop")
    exclude("cascading", "cascading-local"),
  "com.twitter" %% "scalding-args" % scaldingVersion
    exclude("cascading", "cascading-core")
    exclude("cascading", "cascading-hadoop")
    exclude("cascading", "cascading-local"),
  "com.twitter" %% "scalding-date" % scaldingVersion
    exclude("cascading", "cascading-core")
    exclude("cascading", "cascading-hadoop")
    exclude("cascading", "cascading-local"),
  "com.twitter" %% "scalding-commons" % scaldingVersion
    exclude("cascading", "cascading-core")
    exclude("cascading", "cascading-hadoop")
    exclude("cascading", "cascading-local")
    exclude("org.apache.hadoop", "hadoop-core")
    exclude("com.hadoop.gplcompression", "hadoop-lzo")
  // hadoop-lzo also pulled in by elephantbird

  //"com.backtype" % "dfs-datastores" % "1.3.6"
  // "com.twitter" %% "scalding-avro" % scaldingVersion,
  // "com.twitter" %% "scalding-parquet" % scaldingVersion,
)



libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "org.apache.thrift" % "libthrift" % "0.9.1",

  "cascading" % "cascading-core" % cascadingVersion,
  "cascading" % "cascading-hadoop" % cascadingVersion % "provided",
  //"cascading-hbase" % "cascading-hbase-hadoop" % cascadingVersionHbase % "provided" exclude("org.apache.hbase", "hbase-server"),
    /* WARNING: you MUST
          exclude("cascading","cascading-hadoop")
          exclude("cascading-hbase","cascading-hbase-hadoop")
          exclude("org.apache.hadoop", "hadoop-core")

          while importing scalding-utils, and then provide your own backend-switching logic.
     */

  "cascading" % "cascading-local" % cascadingVersion % "provided"

)


libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion
    exclude("com.sun.jersey", "jersey-server"),


  "cascading" % ("cascading-" + cascadingFabric) % cascadingVersion,
  "cascading-hbase" % ("cascading-hbase-" + cascadingFabric) % cascadingVersionHbase exclude("org.apache.hbase", "hbase-server"),

  "cascading" % "cascading-local" % cascadingVersion

) ++ (cascadingFabric match {
    case "hadoop" => Seq(
      "cascading" % "cascading-hadoop" % cascadingVersion % "test"
    )
    case "hadoop2-tez" => Seq(
      "cascading" % "cascading-hadoop" % cascadingVersion % "test",
      "org.apache.tez" % "tez-api" % apacheTezVersion
        exclude ("asm","asm"),
      "org.apache.tez" % "tez-mapreduce" % apacheTezVersion
    )
    case _ => Seq(
      "cascading" % "cascading-hadoop" % cascadingVersion % "test"
    )
  })




credentials += Credentials(Path.userHome / ".ivy2" / "devtools.lan.par.transparencyrights.com.credentials")

publishTo <<= version { (v: String) =>
  val artifactory = "http://devtools.lan.par.transparencyrights.com:8081/artifactory"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at artifactory + "/libs-snapshot-local")
  else
    Some("releases" at artifactory + "/libs-release-local")
}
