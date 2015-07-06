package com.transparencyrights.demo.wcplus

import java.util.Locale

import com.twitter.scalding._
import TDsl._

/**
 * Created by cchepelov on 06/07/15.
 */
class WordCountJob(args: Args) extends CommonJob(args) with Serializable {
  val root = args.getOrElse("root", ".")
  val inputs = args.getOrElse("inputs", root + "/" + "books/")
  val crash = args.getOrElse("crash", "true").toBoolean

  def splitCleanString(s: String) = {
    s.split(Array(' ', '\t', '\n', '.', ',', '-', '"', '”', '?', '!', ';', ':', '¿', '¡', '“', '&', '=', '(', ')', '/',
      '[',']','+','#', '；', '，', '、' ))
      .filterNot(_.isEmpty)
      .map(_.toLowerCase(Locale.US))
  }
  val s = TypedPipe.from(MultipleTextLineFiles(inputs))
    .filterNot(_.trim.isEmpty)

  val wordCounts = s
    .flatMap(x => splitCleanString(x).toSet)
    .groupBy(word => word).mapValues(word => 1).sum
    .toTypedPipe

  if (crash) {
    val maxWordCounts = wordCounts.map(w_c => w_c._2)
      .groupAll.max
      .toTypedPipe.map(u_mc => u_mc._2).sum // sum of 1 iem

    val relatedwc = wordCounts.mapWithValue(maxWordCounts)(
      (w_c: (String, Int), omax: Option[Int]) => (w_c._1, w_c._2.toDouble / omax.get.toDouble))
    
    relatedwc.toPipe[(String, Double)]( ('word, 'relative) ).write(Tsv(root + "/out/" + "rel.csv"))
  }

  wordCounts.toPipe[(String, Int)]( ('word, 'count) ).write(Tsv(root + "/out/" + "abs.csv"))
}
