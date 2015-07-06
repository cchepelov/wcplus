package com.transparencyrights.demo.wcplus



import cascading.tap.SinkMode
import com.twitter.scalding.Args
import com.twitter.scalding.Job
import com.twitter.scalding._

import scala.language.implicitConversions
/**
 * An intermediate class to insert stuff at the job level, for all of our jobs.
 *
 */
abstract class CommonJob(args: Args) extends Job(args) {
  /**
   * Keep 100k tuples in memory by default before spilling
   * Turn this up as high as you can without getting OOM.
   *
   * This is ignored if there is a value set in the incoming jobConf on Hadoop
   */
  override def defaultSpillThreshold: Int = 100 * 1000

  /**
   * Override this in order to set the list of classes to be registered with Kryo
   * @return
   */
  def registerableClasses: Seq[Class[_]] = Nil

  override def config(): Map[AnyRef,AnyRef] = {
    super.config ++
      SroJob.jobConfig(args) ++
      Map[AnyRef, AnyRef](
        "cascading.kryo.accept.all" -> "false", /* true (the default) causes Kryo to accept any serializable even if unregistered (which causes BIG intermediate files) */
        "cascading.kryo.registrations" -> registerableClasses.map(_.getCanonicalName).mkString(":")
      )
  }

  //override def stepStrategy: Option[FlowStepStrategy[_]] = None

  implicit protected val sinkMode = SinkMode.KEEP

  override def name = super.name.replace("com.transparencyrights.sr.online", "c.t.s.o")


  // for test purposes
  val filterOutputPipes = (outputFilter: Option[String], key: String) => {
    outputFilter match {
      case None => true
      case _ => key == outputFilter.get
    }
  }
}

object SroJob {
  def jobConfig(args: Args): Map[AnyRef,AnyRef] = {
    val isTez = args.boolean("hadoop2-tez")
    val noAts = args.boolean("no-ats")

    Map[AnyRef,AnyRef]("mapreduce.map.output.compress" -> "true",


      // "hbase.zookeeper.quorum" -> HBaseConnectionManager.applicableConfig.get("hbase.zookeeper.quorum", "localhostXXX"),

      // "mapred.reduce.child.java.opts" -> "-Xmx513M -Xprof -verbose:gc -Xloggc:/tmp/@taskid@.gc -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp",
      //"mapred.reduce.child.java.opts" -> "-Xmx513M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp",
      //"mapred.map.child.java.opts" -> "-Xmx257M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp",

      "io.compression.codecs" -> "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.BZip2Codec,org.apache.hadoop.io.compress.SnappyCodec",

      "mapreduce.job.queuename" -> args.getOrElse("queue", "default"),
      "tez.queue.name" -> args.getOrElse("queue", "default"),
      "mapred.job.queue.name" -> args.getOrElse("queue", "default"), // legacy
      "mapred.job.queuename" -> args.getOrElse("queue", "default"), // legacy

      /* EXPERIMENTAL cchepelov 06MAY15 following ckw/sve discussion: restore pre-2.6.0 behaviour with 'identical' taps */
      "cascading.multimapreduceplanner.collapseadjacentaps" -> "false",
      /* -- END EXPERIMENTAL -- */

      "mapreduce.output.fileoutputformat.compress.codec" -> "org.apache.hadoop.io.compress.SnappyCodec",
      "mapreduce.output.fileoutputformat.compress.type" -> "BLOCK",

      "mapreduce.job.jvm.numtasks" -> "1000" /* default is 1 a.k.a 'no reuse'. Let's see if we can ge some mileage
                                                   out of long-running JVM's */
      // "keep.failed.task.files" -> "true",

    ) ++ (if (isTez) Map(


      /* suggested by Rasjesh Balamohan in TEZ-2237: */
      // "tez.task.scale.memory.ratios" -> "4,1,1,10,10,1,1",  //  (4 indicates the ratio allocated for unorderedpartitioned case, default would be 1).

      /* as suggested by [~sseth] in https://issues.apache.org/jira/browse/TEZ-2237?focusedCommentId=14387870&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14387870 */
      // "tez.am.dag.scheduler.class" -> "org.apache.tez.dag.app.dag.impl.DAGSchedulerNaturalOrderControlled",
      // did nothing.

      "tez.task.launch.cmd-opts" -> (
        //"-XshowSettings -Xdiag -verbose:class " +
        //"-XX:+TraceClassLoading -XX:+TraceLoaderConstraints " + "-XX:+TraceClassResolution " +
        "-XX:+UnlockDiagnosticVMOptions -XX:NativeMemoryTracking=summary -XX:+PrintNMTStatistics " +

          "-XX:+AggressiveOpts " +
          "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseParallelGC "
          + " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"),
      "tez.generate.debug.artifacts" -> "true", // default false

      // "tez.task.resource.memory.mb" -> "1024", // default 1024
      // "tez.task.resource.cpu.vcores" -> "1", // default 1

      // this will be fun:
      // "tez.am.speculation.enabled" -> "true", // default false

      "tez.task.resource.memory.mb" -> (1024+512).toString, // default 1024
      "tez.container.max.java.heap.fraction" -> "0.7", // default 0.8

      "tez.am.mode.session" -> "true",
      "tez.am.container.idle.release-timeout-min.millis" -> "10000",
      "tez.am.container.idle.release-timeout-max.millis" -> "60000",

      //"tez.task.resource.memory.mb" -> "2048", // default 1024
      //"tez.container.max.java.heap.fraction" -> "0.8", // default 0.8

      "tez.tez-ui.history-url.base" -> "http://localhost:9111",


      /* WTF? This is something for Tez apparently. Oh, hai, warning: this ALSO impacts MapReduce. */
      "cascading.flow.runtime.gather.partitions.num" -> args.getOrElse("tez-partitions","4"), // TODO move to Parameters
      "tez.runtime.intermediate-output.should-compress" -> "true",
      "tez.runtime.intermediate-input.is-compressed" -> "true",
      "tez.lib.uris" -> args.getOrElse("tez.lib.uris", "hdfs://tpcy-par/apps/tez-0.6.1/tez-0.6.1-SNAPSHOT-rp5.tar.gz")

    ) else Map()) ++ (if (!noAts) Map(
      "yarn.timeline-service.hostname" -> "orc2.lan.par.transparencyrights.com",
      "tez.history.logging.service.class" -> "org.apache.tez.dag.history.logging.ats.ATSHistoryLoggingService", /* Logging into the Timeline server */
      "tez.allow.disabled.timeline-domains" -> "true" /* override security for logging */
    ) else Map())
  }


}
