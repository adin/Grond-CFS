package grond.model;
import java.lang.{Long => JLong, Integer => JInteger, Double => JDouble}
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.api.datastore._

object doctorUtil {
  /** Recalculate the doctor's average rate, type and other rating-dependent fields. */
  def updateFromRatings (doctorKey: Key): Unit = {
    val query = new Query ("DoctorRating", doctorKey)
    val fmDeltas = new mutable.ListBuffer[Int]
    val cfsDeltas = new mutable.ListBuffer[Int]
    val typesCount = new mutable.HashMap[String, Int]
    for (rating <- util.queryToList (query)) {
      val problem = (rating getProperty "problem").asInstanceOf[String]
      val actLevStart = (rating getProperty "actLevStart").asInstanceOf[String]
      val actLevEnd = (rating getProperty "actLevEnd").asInstanceOf[String]
      if (problem != null && actLevStart != null && actLevEnd != null) {
        val activityRating = Integer.parseInt (actLevEnd) - Integer.parseInt (actLevStart)
        problem match {
          case "fm" => fmDeltas += activityRating
          case "cfs" => cfsDeltas += activityRating
          case unknown => println ("calculateMedianRating: Unknown condition `" + unknown + "` in " + rating.getKey)
        }
      }

      val types = (rating getProperty "type").asInstanceOf[ju.List[String]]
      if (types != null) for (ti <- types) {typesCount.update (ti, typesCount.getOrElse (ti, 0) + 1)}
    }
    val doctor = Datastore.SERVICE.get (doctorKey)
    def calcMedian (deltas: mutable.ListBuffer[Int]): Double = {
      if (deltas.isEmpty) 0.0
      else deltas.sorted (math.Ordering.Int) (deltas.size / 2) .toDouble
    }
    def saveMedian (field: String, deltas: mutable.ListBuffer[Int]) = {
      if (deltas.isEmpty) doctor.removeProperty (field)
      else doctor.setProperty (field, calcMedian (deltas))
    }
    saveMedian ("fmRating", fmDeltas)
    saveMedian ("cfsRating", cfsDeltas)
    val typesByCount: ju.Collection[AnyRef] = typesCount.keys.toList.sort {case (a, b) =>
      typesCount(b) < typesCount(a) || a < b} .flatMap (k => List(k, new java.lang.Integer (typesCount (k))))
    doctor.setProperty ("_type", typesByCount)
    Datastore.SERVICE.put (doctor)
  }
}