package grond.model;
import java.lang.{Long => JLong, Integer => JInteger, Double => JDouble}
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.repackaged.org.json.{JSONObject}
import com.google.appengine.api.datastore.{Key, KeyFactory}
import grond.shared.{Doctor, DoctorRating}

object doctorUtil {
  protected def fetchRatings (doctor: Doctor): (Iterable[DoctorRating], Iterable[DoctorRating]) = {
    val query = OFY.query (classOf[DoctorRating]) .ancestor (doctor)
    query.partition (_ isFinished)
  }

  /** Recalculate the doctor's average rate, type and other rating-dependent fields. */
  def updateFromRatings (doctor: Doctor): Unit = {
    val (ratings, unfinishedRatings) = fetchRatings (doctor)

    // Average Satisfaction
    try {
      val fmSatisfaction = new mutable.ListBuffer[Int]
      val cfsSatisfaction = new mutable.ListBuffer[Int]
      for (rating <- ratings) {
        if (rating.satAfter > 0) {
          val sat = rating.satAfter
          if (rating.condition != null && sat >= 1 && sat <= 10) {
            rating.condition match {
              case "fm" => fmSatisfaction += sat
              case "cfs" => cfsSatisfaction += sat
              case unknown => println ("calculateMedianRating: Unknown condition `" + unknown + "` in " + rating)
            }
          }
        }
      }
      def calcAverage (ratings: mutable.ListBuffer[Int]): Int = {
        if (ratings.isEmpty) 0
        else ratings.sum / ratings.size
      }
      doctor._fmSatisfaction = calcAverage (fmSatisfaction)
      doctor._cfsSatisfaction = calcAverage (cfsSatisfaction)
    } catch {case ex => ex.printStackTrace}

    // Type
    try {
      val typesCount = new mutable.HashMap[String, Int]
      for (rating <- ratings) {
        if (rating.`type` != null) for (ti <- rating.`type`) {typesCount.update (ti, typesCount.getOrElse (ti, 0) + 1)}
      }
      val lhm = new ju.LinkedHashMap[String, java.lang.Integer]
      for (sortedKey <- typesCount.keys.toList.sortWith {case (a, b) => typesCount(b) < typesCount(a) || a < b}) {
        lhm.put (sortedKey, typesCount (sortedKey))
      }
      doctor.`type` = lhm
    } catch {case ex => ex.printStackTrace}

    // Experience
    try {
      val experienceLevels = DoctorRating.experienceLevels.toList
      val experience = ratings.map (_.experience) .filter (_ ne null) .map (experienceLevels indexOf _) .filter (_ >= 0)
      doctor._experience = if (experience.isEmpty) null else experienceLevels (experience.sum / experience.size)
    } catch {case ex => ex.printStackTrace}

    // Number of reviews
    doctor._numberOfReviews = ratings.size

    // Average cost
    try {
      // The ranges used in the "averageCost" might change with time.
      // In order to draw sound conclusions from the variating ranges, we sample each range with a set of fixed prices.
      val samples = Map ((300, 1), (900, 2), (1500, 3), (3600, 4), (7200, 5))
      val ranges = ratings.map (_.averageCost) .filter (_ ne null)
      val levels = ranges.flatMap {case range =>
        // The range has a "<N", ">N" or "N1-N2" form.
        val (begin, end) = if (range startsWith "<") (0, range.substring(1).toInt)
          else if (range startsWith ">") (range.substring(1).toInt, Integer.MAX_VALUE)
          else {val hyphen = range.indexOf ('-'); (range.substring (0, hyphen) .toInt, range.substring (hyphen + 1) .toInt)}
        samples.find {case (sample, level) => begin <= sample && sample <= end} .map (_._2)
      }
      doctor._averageCostLevel = if (levels.isEmpty) 0 else levels.sum / levels.size
    } catch {case ex => ex.printStackTrace}

    // Denormalized list of ratings, used to detect if the current user have rated the doctor.
    try {
      doctor.ratings = seqAsJavaList[String] (for (rating <- ratings.toList ++ unfinishedRatings) yield {
        val json = new JSONObject
        json.put ("id", rating.id)
        val crc32 = new java.util.zip.CRC32
        crc32.update (rating.user getBytes "UTF-8")
        json.put ("userHash", crc32.getValue) // `_ratings` is public, therefore we're hiding the user ids behind CRC32.
        json.put ("finished", ratings contains rating)
        json.put ("condition", rating.condition)
        json.toString
      })
    } catch {case ex => ex.printStackTrace}

    OFY.put[Doctor] (doctor)
  }

  def getTRPInfo (doctor: Doctor): JSONObject = {
    val json = new JSONObject
    val (ratings, unfinishedRatings) = fetchRatings (doctor)

    def percent (field: DoctorRating => String, value: String): Unit = try {
      val count = ratings.count (field (_) == value)
      json.put (field + "Percent", 100.0 * count / ratings.size)
    } catch {case ex => ex.printStackTrace}

    percent (_.insurance, "Yes")
    percent (_.ripoff, "Yes")

    def percentSpread (field: DoctorRating => Iterable[String]): Unit = try {
      val spread = ratings.flatMap (field (_)).groupBy (s => s)
      val sorted = spread.map {case (value, values) => (value, values.size)} .toList.sortBy (_._2)
      val percent = sorted.flatMap {case (value, size) => List (value, 100.0 * size / ratings.size)}
      json.put (field + "PercentSpread", seqAsJavaList[Any] (percent))
    } catch {case ex => ex.printStackTrace}

    percentSpread (_.treatmentBreadth)

    try {
      val nums = ratings.map (_.actLevStart) .filter (_ > 0)
      if (nums.size > 0) {
        val sum = nums.map (_.toInt) .sum
        json.put ("actLevStart_average", sum / nums.size)
      }
    } catch {case ex => ex.printStackTrace}
    
    try {
      val average = for (field <- DoctorRating.levelPrefixes; suffix <- "Before" :: "After" :: Nil; val name = field + suffix) yield {
        val values = ratings.map (_.levels.get (name)) .filter (_ != null) .map (_.intValue)
        (name, if (values.isEmpty) null else values.sum / values.size)
      }
      json.put ("averagePatientCondition", mapAsJavaMap (Map (average :_*)))
    } catch {case ex => ex.printStackTrace}

    try {
      val averageGain = DoctorRating.levelPrefixes.map {case field =>
        val gains = ratings.flatMap {case rating =>
          val before = rating.levels.get (field + "Before") match {case null => None; case num => Some (num.intValue)}
          val after = rating.levels.get (field + "After") match {case null => None; case num => Some (num.intValue)}
          if (before.isDefined && after.isDefined) Some (after.get - before.get) else None
        }
        (field, if (gains.isEmpty) 0 else gains.sum / gains.size)
      }
      json.put ("averagePatientGain", mapAsJavaMap (Map (averageGain :_*)))
    } catch {case ex => ex.printStackTrace}

    // XXX: Implement all-ratings calculations as a background / cron task.

    lazy val allFinishedRatings = OFY.query (classOf[DoctorRating]) .filter ("satAfter >", 0) .toList

    try {
      // Average condition for ALL doctors.
      val average = for (field <- DoctorRating.levelPrefixes; suffix <- "Before" :: "After" :: Nil; val name = field + suffix) yield {
        val values = allFinishedRatings.map (_.levels get name) .filter (_ != null) .map (_.intValue)
        (name, if (values.isEmpty) null else values.sum / values.size)
      }
      json.put ("averageAllPatientsCondition", mapAsJavaMap (Map (average :_*)))
    } catch {case ex => ex.printStackTrace}

    json
  }
}
