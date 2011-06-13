package grond.model;
import java.lang.{Long => JLong, Integer => JInteger, Double => JDouble}
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.api.datastore._

object doctorUtil {
  /** Recalculate the doctor's average rate, type and other rating-dependent fields. */
  def updateFromRatings (doctorKey: Key): Unit = {
    val query = new Query ("DoctorRating", doctorKey)
    query.addFilter ("satAfter", Query.FilterOperator.NOT_EQUAL, "") // Rating is finished if satAfter is set.
    val ratings = util.queryToList (query)
    val doctor = Datastore.SERVICE.get (doctorKey)

    // Average Satisfaction
    try {
      val fmSatisfaction = new mutable.ListBuffer[Int]
      val cfsSatisfaction = new mutable.ListBuffer[Int]
      for (rating <- ratings) {
        val problem = (rating getProperty "problem").asInstanceOf[String]
        val satAfter = (rating getProperty "satAfter").asInstanceOf[String]
        if (satAfter != null) {
          val sat = Integer.parseInt (satAfter)
          if (problem != null && sat >= 1 && sat <= 10) {
            problem match {
              case "fm" => fmSatisfaction += sat
              case "cfs" => cfsSatisfaction += sat
              case unknown => println ("calculateMedianRating: Unknown condition `" + unknown + "` in " + rating.getKey)
            }
          }
        }
      }
      def calcAverage (ratings: mutable.ListBuffer[Int]): Int = {
        if (ratings.isEmpty) 0
        else ratings.sum / ratings.size
      }
      def saveAverage (field: String, ratings: mutable.ListBuffer[Int]) = {
        if (ratings.isEmpty) doctor.removeProperty (field)
        else doctor.setProperty (field, calcAverage (ratings))
      }
      saveAverage ("_fmSatisfaction", fmSatisfaction)
      saveAverage ("_cfsSatisfaction", cfsSatisfaction)
    } catch {case ex => ex.printStackTrace}

    // Type
    try {
      val typesCount = new mutable.HashMap[String, Int]
      for (rating <- ratings) {
        val types = (rating getProperty "type").asInstanceOf[ju.List[String]]
        if (types != null) for (ti <- types) {typesCount.update (ti, typesCount.getOrElse (ti, 0) + 1)}
      }
      val typesByCount: ju.Collection[AnyRef] = typesCount.keys.toList.sortWith {case (a, b) =>
        typesCount(b) < typesCount(a) || a < b} .flatMap (k => List(k, new java.lang.Integer (typesCount (k))))
      doctor.setProperty ("_type", typesByCount)
    } catch {case ex => ex.printStackTrace}

    // Experience
    try {
      val experienceLevels = List ("Harmful", "No Help", "Learner", "Informed", "Expert", "Specialist")
      val experience = ratings.map (_.getProperty("experience").asInstanceOf[String]) .filter (_ ne null)
        .map (experienceLevels indexOf _) .filter (_ != -1)
      if (!experience.isEmpty) {
        val expAvg = experience.sum / experience.size
        doctor.setProperty ("_experience", experienceLevels (expAvg))
      }
    } catch {case ex => ex.printStackTrace}

    // Number of reviews
    doctor.setProperty ("_numberOfReviews", ratings.size)

    Datastore.SERVICE.put (doctor)
  }
}