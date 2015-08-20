organization := "com.transparencyrights.demo"
name := "wcplus"
version := "0.0-SNAPSHOT"

scalaVersion := "2.11.7"
scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")


val scaldingVersion = "0.13.1-cch-ffc2"


val defaultCascadingVersion = "3.0.2-wip-146"


//val defaultCascadingVersion = "3.0.2-wip-dev" // from July 23rd  // OK ON TEZ
//val defaultCascadingVersion = "3.0.2-wip-140" // fails, 20 nodes
// val defaultCascadingVersion = "3.1.0-wip-4"


//val defaultCascadingVersion = "3.0.2-wip-139" // fails, 20 nodes
//val defaultCascadingVersion = "3.0.2-wip-136"
//val defaultCascadingVersion = "3.0.2-wip-dev"
//val defaultCascadingVersion = "3.0.1"
//val defaultCascadingVersion = "2.6.3"

//val defaultCascadingVersion = "2.6.3"

val cascadingVersion = sys.props.getOrElse("CASCADING_VERSION", defaultCascadingVersion)

val cascadingVersionHbase = "3.0.0"

val hadoopVersion = "2.6.0"
val apacheTezVersion = "0.6.2"

val cascadingFabric = sys.props.getOrElse("CASCADING_FABRIC", "hadoop2-tez") // can be "hadoop", "hadoop2-mr1" or "hadoop2-tez"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
    "cascading" % "cascading-hadoop2-io" % cascadingVersion
    
    ///"org.apache.hadoop" % "hadoop-common" % "2.7.1",
    // "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.7.1"
)

libraryDependencies ++= (if (cascadingVersion.endsWith("-dev")) Seq(
	"org.jgrapht" % "jgrapht-core" % "0.9.1",
    "org.jgrapht" % "jgrapht-ext" % "0.9.1",
    "riffle" % "riffle" % "1.0.0-wip-7",
    "org.codehaus.janino" % "janino" % "2.7.6"
) else Nil)


libraryDependencies ++= Seq(
  "aaaaa.acmecorp" %% "guava-fix" % "1.0.4",
  "aaaaa.acmecorp" %% "guava-fix" % "1.0.4" % "test",
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
)



libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % "0.9.1",

  "cascading" % "cascading-core" % cascadingVersion,
  "cascading" % "cascading-hadoop" % cascadingVersion % "provided",
  //"cascading-hbase" % "cascading-hbase-hadoop" % cascadingVersionHbase % "provided" exclude("org.apache.hbase", "hbase-server"),

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
      "org.apache.tez" % "tez-mapreduce" % apacheTezVersion,
      "org.apache.tez" % "tez-dag" % apacheTezVersion
    )
    case _ => Seq(
      "cascading" % "cascading-hadoop" % cascadingVersion % "test"
    )
  })

ivyXML :=
  <dependencies>
    <exclude org="tomcat" name="jasper-compiler" />
    <exclude org="tomcat" name="jasper-runtime" />
    <exclude org="tomcat" name="jsp-api" />
    <exclude org="org.mortbay.jetty" name="jsp-api" />
    <exclude org="org.mortbay.jetty" name="jsp-api-2.1" />
    <exclude org="org.mortbay.jetty" name="jsp-2.1" />
    <exclude org="org.mortbay.jetty" name="servlet-api-2.5" />
    <exclude org="javax.servlet" name="servlet-api" />
    <exclude org="javax.servlet" name="jsp-api" />
    <exclude org="javax.servlet.jsp" name="servlet-api" />

    <exclude org="org.apache.hadoop" name="hadoop-yarn-client" /> <!-- 2.6.0 -->
    <exclude org="asm" name="asm" />
    
    <exclude org="stax" name="stax-api" />
  </dependencies>


