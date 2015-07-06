logLevel := Level.Warn

// resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.gilt" % "sbt-dependency-graph-sugar" % "0.7.5-1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")
