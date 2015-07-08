package com.transparencyrights.demo.wcplus

import java.util.Locale

import com.twitter.algebird.Semigroup
import com.twitter.scalding._
import TDsl._
import com.twitter.scalding.mathematics.Histogram
import com.twitter.scalding.typed.ValuePipe
import com.typesafe.scalalogging.LazyLogging

case class StringAndCount(value: String, count: Int) extends Serializable { }
case class StringAndScore(value: String, score: Double) extends Serializable { }
case class StringAndScoreMed(value: String, score: Double, distanceToMedian: Double) extends Serializable { }
object StringAndScoreMed {
  def apply(sas: StringAndScore, median: Double): StringAndScoreMed = {
    StringAndScoreMed(value = sas.value, score = sas.score, distanceToMedian = Math.abs(sas.score - median))
  }
}

/**
 * Created by cchepelov on 02/07/15.
 */
class MakeWordQueriesJob(args: Args) extends CommonJob(args) with Serializable with LazyLogging {
  val root = args.getOrElse("root", ".")
  val inputs = args.getOrElse("inputs", root + "/" + "books")
  val doFilter = args.getOrElse("filter", "false").toBoolean
  val doMedian = ! args.getOrElse("fakeMedian", "false").toBoolean
  val manygrams = args.getOrElse("manygrams", "5").toInt

  def splitCleanString(s: String) = {
    s.split(Array(' ', '\t', '\n', '.', ',', '-', '"', '”', '?', '!', ';', ':', '¿', '¡', '“', '&', '=', '(', ')', '/',
      '[',']','+','#', '；', '，', '、' ))
      .filterNot(_.isEmpty)
      .map(_.toLowerCase(Locale.US))
  }

  val s = TypedPipe.from(TextLine(inputs))

  val text = (if (isTez) {
      s.filterNot(_.trim.isEmpty)
        .forceToDisk // Tez trick; or most of the work happens in a single node (bad). Measured to be a wash on hadoop
            // conditionalizing the .forceToDisk to remain "on the safe side" for now.
    } else {
      s.filterNot(_.trim.isEmpty)
    })
    .map(x => splitCleanString(x))

  def nGrams(size: Int) = size match {
    case 1 => text.flatMap(s => s.toSet)
    case _ => text.flatMap(s => s.sliding(size).map(x => x.mkString(" ")).toSet)
  }

  def makeOutput(size: Int, externalThreshold: Option[ValuePipe[Int]]) = {

    val counts = nGrams(size)
      .groupBy( x => x )
      .mapValues( _ => 1 ).sum
      .toTypedPipe
      .map(x => StringAndCount(x._1, x._2))

    lazy val maxCounts = counts.map(_.count).sum(new Semigroup[Int] with Serializable {
      override def plus(l: Int, r: Int): Int = Math.max(l,r)
    })

    val threshold = maxCounts.map(x => x*8 / 10)

    /* if the following line is commented out, Cascading 3.0.1 will barf. */
    val threshold_save = maxCounts.toPipe('count).write(Tsv(root + "/out/" + s"maxcount_${size}.csv"))

    lazy val usingThreshold = externalThreshold.getOrElse(threshold)



    val filteredCounts = (if (doFilter) {
      counts.filterWithValue(usingThreshold)( (v: StringAndCount, omax: Option[Int]) => {
        (v.count <= omax.get)
      })
      .mapWithValue(maxCounts)( (v: StringAndCount, omax: Option[Int]) => {
        val maxf: Double = omax.get
        StringAndScore(v.value, v.count / maxf)
      })
  } else {
      counts.map(xx => StringAndScore(xx.value, xx.count.toDouble))
    })

    val filteredSortedCounts = filteredCounts
      .groupBy(_.score) /* implicit sort through grouping */
      .toTypedPipe
      .map(_._2) /* drop "sort" key */

    filteredSortedCounts.map(x => (x.value, x.score)).toPipe( ('expression, 'relfreq )).write(Tsv(root + "/out/" + s"words_${size}.csv"))

    val hist = filteredCounts.map(x => (x.value, x.score)).toPipe( ('expression, 'relfreq) ).groupAll( _.histogram('relfreq -> 'hist ))

    val medianValue  = hist
      .mapTo('hist -> 'median) { h: Histogram => h.median }
      .toTypedPipe[(Double)]( ('median) )
      .sum // make it a ValuePipe

    (threshold, medianValue, filteredCounts)
  }

  val (threshold1, medianValue1, filteredCounts1) = makeOutput(1, None)
  val tmf = Seq( (threshold1, medianValue1, filteredCounts1) ) ++
      (2 to manygrams).map(size => makeOutput(size, Some(threshold1)))

  val lanesRelativeToMedian = (if (doMedian) {
    /* real code */
    tmf
      .collect { case (t, m, fc) => fc.mapWithValue(m)( (v: StringAndScore, omed: Option[Double]) => StringAndScoreMed(v, omed.get)
    )
    }
  } else {
    /* workaround code */
    tmf.collect { case (t,m,fc) => fc.map(v => StringAndScoreMed(v, v.score)) }
  })


  lanesRelativeToMedian.zipWithIndex.foreach { case (sasPipe, index) => {
    sasPipe.map(x => (x.value, x.score, x.distanceToMedian))
      .toPipe( ('expression, 'score, 'distanceToMedian) )
      .write(Tsv(root + "/out/" + s"lane_${index + 1}.csv"))
  } }

  val globalRelativeToMedian = lanesRelativeToMedian.reduce(_ ++ _) // we're reducing a list of pipes (driver-side)

  val sortedRelativeToMedian = globalRelativeToMedian
      .groupBy(_.distanceToMedian)
      .toTypedPipe.map(_._2)

  sortedRelativeToMedian
    .map(x => (x.value, x.score, x.distanceToMedian))
    .toPipe( ('expression, 'score, 'distanceToMedian) )
    .write(Tsv(root + "/out/"+ "expressions.csv"))
}