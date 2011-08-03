package grond.model;
import java.lang.{Long => JLong, Integer => JInteger, Double => JDouble}
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.repackaged.org.json.{JSONObject}
import com.google.appengine.api.datastore._

object doctorUtil {
  protected def fetchRatings (doctorKey: Key): (Seq[Entity], Seq[Entity]) = {
    val query = new Query ("DoctorRating", doctorKey)
    util.queryToList (query) .partition {case ratingEntity =>
      val finished = ratingEntity.getProperty ("satAfter") match {case null|"" => false; case _ => true}
      finished
    }
  }

  /** Recalculate the doctor's average rate, type and other rating-dependent fields. */
  def updateFromRatings (doctorKey: Key): Unit = {
    val (ratings, unfinishedRatings) = fetchRatings (doctorKey)
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
      val experience = ratings.map (_.getProperty ("experience") .asInstanceOf[String]) .filter (_ ne null)
        .map (experienceLevels indexOf _) .filter (_ != -1)
      if (experience.isEmpty) doctor.removeProperty ("_experience")
      else doctor.setProperty ("_experience", experienceLevels (experience.sum / experience.size))
    } catch {case ex => ex.printStackTrace}

    // Number of reviews
    doctor.setProperty ("_numberOfReviews", ratings.size)

    // Average cost
    try {
      // The ranges used in the "averageCost" might change with time.
      // In order to draw sound conclusions from the variating ranges, we sample each range with a set of fixed prices.
      val samples = Map ((300, 1), (900, 2), (1500, 3), (3600, 4), (7200, 5))
      val ranges = ratings.map (_.getProperty ("averageCost") .asInstanceOf[String]) .filter (_ ne null)
      val levels = ranges.flatMap {case range =>
        // The range has a "<N", ">N" or "N1-N2" form.
        val (begin, end) = if (range startsWith "<") (0, range.substring(1).toInt)
          else if (range startsWith ">") (range.substring(1).toInt, Integer.MAX_VALUE)
          else {val hyphen = range.indexOf ('-'); (range.substring (0, hyphen) .toInt, range.substring (hyphen + 1) .toInt)}
        samples.find {case (sample, level) => begin <= sample && sample <= end} .map (_._2)
      }
      if (levels.isEmpty) doctor.removeProperty ("_averageCostLevel")
      else doctor.setProperty ("_averageCostLevel", levels.sum / levels.size)
    } catch {case ex => ex.printStackTrace}

    // Denormalized list of ratings, used to detect if the current user have rated the doctor.
    try {
      doctor.setProperty ("_ratings", asJavaList[String] (for (rating <- ratings ++ unfinishedRatings) yield {
        val json = new JSONObject
        json.put ("key/id", rating.getKey().getId())
        val crc32 = new java.util.zip.CRC32
        crc32.update ((rating getProperty "user").asInstanceOf[String] getBytes "UTF-8")
        json.put ("userHash", crc32.getValue) // `_ratings` is public, therefore we're hiding the user ids behind CRC32.
        json.put ("finished", ratings contains rating)
        json.put ("problem", (rating getProperty "problem").asInstanceOf[String])
        json.toString
      }))
    } catch {case ex => ex.printStackTrace}

    Datastore.SERVICE.put (doctor)
  }

  def getTRPInfo (doctorKey: Key): JSONObject = {
    val json = new JSONObject
    val (ratings, unfinishedRatings) = fetchRatings (doctorKey)

    def percent (field: String, value: String): Unit = try {
      val count = ratings.count (_.getProperty (field) .asInstanceOf[String] == value)
      json.put (field + "Percent", 100.0 * count / ratings.size)
    } catch {case ex => ex.printStackTrace}

    percent ("insurance", "Yes")
    percent ("ripoff", "Yes")

    def percentSpread (field: String): Unit = try {
      val spread = ratings.flatMap (_.getProperty (field) match {
        case null => Nil
        case col: ju.Collection[Any] => col // Multiple values in the field.
        case value: String => value :: Nil // Single value.
      }).groupBy (s => s)
      val sorted = spread.map {case (value, values) => (value, values.size)} .toList.sortBy (_._2)
      val percent = sorted.flatMap {case (value, size) => List (value, 100.0 * size / ratings.size)}
      json.put (field + "PercentSpread", asJavaList[Any] (percent))
    } catch {case ex => ex.printStackTrace}

    percentSpread ("treatmentBreadth")

    try {
      val nums = ratings.map (_.getProperty ("actLevStart") .asInstanceOf[String]) .filter (_ != null)
      if (nums.size > 0) {
        val sum = nums.map (_.toInt) .sum
        json.put ("actLevStart_average", sum / nums.size)
      }
    } catch {case ex => ex.printStackTrace}
    
    try {
      val average = for (field <- grond.shared.Fields.levelPrefixes; suffix <- "Before" :: "After" :: Nil; val name = field + suffix) yield {
        val values = ratings.map (_.getProperty (name) .asInstanceOf[String]) .filter (value => value != null && value.length != 0) .map (_.toInt)
        (name, if (values.isEmpty) null else values.sum / values.size)
      }
      json.put ("averagePatientCondition", asJavaMap (Map (average :_*)))
    } catch {case ex => ex.printStackTrace}

    try {
      val averageGain = grond.shared.Fields.levelPrefixes.map {case field =>
        val gains = ratings.flatMap {case rating =>
          val before = rating.getProperty (field + "Before") .asInstanceOf[String] match {case null|"" => None; case num => Some (num.toInt)}
          val after = rating.getProperty (field + "After") .asInstanceOf[String] match {case null|"" => None; case num => Some (num.toInt)}
          if (before.isDefined && after.isDefined) Some (after.get - before.get) else None
        }
        (field, if (gains.isEmpty) 0 else gains.sum / gains.size)
      }
      json.put ("averagePatientGain", asJavaMap (Map (averageGain :_*)))
    } catch {case ex => ex.printStackTrace}

    // XXX: Implement all-ratings calculations as a background / cron task.

    lazy val allFinishedRatings = util.queryToList (new Query ("DoctorRating"))
      .filter (_.getProperty ("satAfter") match {case null|"" => false; case _ => true})

    try {
      // Average condition for ALL doctors.
      val average = for (field <- grond.shared.Fields.levelPrefixes; suffix <- "Before" :: "After" :: Nil; val name = field + suffix) yield {
        val values = allFinishedRatings.map (_.getProperty (name) .asInstanceOf[String]) .filter (value => value != null && value.length != 0) .map (_.toInt)
        (name, if (values.isEmpty) null else values.sum / values.size)
      }
      json.put ("averageAllPatientsCondition", asJavaMap (Map (average :_*)))
    } catch {case ex => ex.printStackTrace}

    json
  }
}
