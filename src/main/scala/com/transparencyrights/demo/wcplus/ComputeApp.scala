package com.transparencyrights.demo.wcplus

import com.twitter.scalding.{Job, Args, CascadeJob}

/**
 * Created by cchepelov on 02/07/15.
 */
class ComputeApp(args: Args) extends CascadeJob(args) {

  /* FIXME: this is really the CascadeJob#run code except calling a different overload from CascadeConnector. Need to make a PR*/
  override def run = {
    import scala.collection.JavaConverters._

    val flows = jobs.map { _.buildFlow }
    val cascade = new cascading.cascade.CascadeConnector(config.asJava).connect(flows: _*)
    preProcessCascade(cascade)
    cascade.complete()
    postProcessCascade(cascade)
    val statsData = cascade.getCascadeStats
    //handleStats(statsData) // CascadingStats sprouted a [T] type parameter from 2.6.x→3.0.0, scalding no longer builds.
    statsData.isSuccessful
  }

  override def jobs(): Seq[Job] = {
    Seq[Job](new WordCountJob(args) /*,  new MakeWordQueriesJob(args) */)
  }

}
