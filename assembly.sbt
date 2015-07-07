import java.security.MessageDigest

import sbtassembly.{AssemblyUtils, MergeStrategy}

val takeGuavaFixMergeStrategy = new MergeStrategy {
  val name = "takeGuavaFix"

  private def filenames(tempDir: File, fs: Seq[File]): Seq[String] =
    for(f <- fs) yield {
      AssemblyUtils.sourceOfFileForMerge(tempDir, f) match {
        case (path, base, subDirPath, false) => subDirPath
        case (jar, base, subJarPath, true) => jar + ":" + subJarPath
      }
    }
  private def sha1content(f: File): String = bytesToSha1String(IO.readBytes(f))
  private def bytesToSha1String(bytes: Array[Byte]): String =
    bytesToString(sha1.digest(bytes))
  private def bytesToString(bytes: Seq[Byte]): String =
    bytes map {"%02x".format(_)} mkString
  private def sha1 = MessageDigest.getInstance("SHA-1")

  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    files.foreach(x => {
      AssemblyUtils.sourceOfFileForMerge(tempDir, x) match {
        case (_,_,_, false) => System.out.println("before filter: ", x.getPath)
        case (jar, base, p, true) => System.out.println(s"before filter:  jar=$jar base=$base p=$p x=${x.getPath}")
      }
    })


    val filteredFiles = files.filter(f => {
      AssemblyUtils.sourceOfFileForMerge(tempDir, f) match {
        case (_,_,_, false) => true // local file, OK (trumps everything)
        case (jar, base, p, true) => jar.getName.contains("guava-fix")
      }
    })

    filteredFiles.foreach(x => {
      AssemblyUtils.sourceOfFileForMerge(tempDir, x) match {
        case (_,_,_, false) => System.out.println("after filter: ", x.getPath)
        case (jar, base, p, true) => System.out.println(s"after filter:  jar=$jar base=$base p=$p x=${x.getPath}")
      }
    })

    if (filteredFiles.length == 1) {
      Right(Seq(filteredFiles.head -> path)) // if "head" explodes, you DON'T have guava-fix or a local override.
    } else {
      val fingerprints = Set() ++ (filteredFiles map (sha1content))
      if (fingerprints.size == 1) Right(Seq(filteredFiles.head -> path)) // good enough
      else {
        Left("even after filtering, found multiple files for same target path:" +
          filenames(tempDir, filteredFiles).mkString("\n", "\n", ""))
      }
    }

  }
}

assemblyMergeStrategy in assembly := {
    case PathList(ps @ _*) if ps.last == "pom.properties" => MergeStrategy.rename
    case PathList(ps @ _*) if ps.last == "pom.xml" => MergeStrategy.rename

    case PathList("javax", "el", cl @_*) => MergeStrategy.rename

    case PathList("com", "google", "common", "base", "Stopwatch$1.class") => takeGuavaFixMergeStrategy // first is aaaaa.acmecorp
    case PathList("com", "google", "common", "base", "Stopwatch.class") => takeGuavaFixMergeStrategy
    case PathList("com", "google", "common", "io", "Closeables.class") => takeGuavaFixMergeStrategy

    case PathList("org", "apache", "hadoop", "yarn", "factories", "package-info.class") => MergeStrategy.first
    case PathList("org", "apache", "hadoop", "yarn", "factory", "providers", "package-info.class") => MergeStrategy.first
    case PathList("org", "apache", "hadoop", "yarn", "util", "package-info.class") => MergeStrategy.first

    case PathList("core-default.xml") => MergeStrategy.first
    case PathList("hdfs-default.xml") => MergeStrategy.first
    case PathList("mapred-default.xml") => MergeStrategy.first

    case PathList("org", "apache","hadoop", "hbase","mapred","TableInputFormatBase.class") => MergeStrategy.first

    case PathList("org", "apache", "jasper", xs @ _*) => MergeStrategy.rename

    case PathList("project.clj") => MergeStrategy.first

   	case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first    
    case PathList("com", "esotericsoftware", "minlog", ps @ _*) => MergeStrategy.first 
    //case "application.conf" => MergeStrategy.concat
    //case "unwanted.txt"     => MergeStrategy.discard

   case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
 }

