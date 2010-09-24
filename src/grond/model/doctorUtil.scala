package grond.model;
import java.lang.{Long => JLong, Integer => JInteger, Double => JDouble}
import java.util.{Map => JMap, HashMap => JHashMap, List => JList, LinkedList => JLinkedList}
import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import com.google.appengine.api.datastore._

/** Copy of the DoctorRating data being kept in the Doctor. */
case class DenormalizedRating (@BeanProperty var actLevStart: Int, @BeanProperty var actLevEnd: Int) {
  def this () = this (0, 0) // Default constructor for flexjson.
  def activityRating = actLevEnd - actLevStart
}

object doctorUtil {
  type RatingsCopy = JHashMap[String, JHashMap[String, DenormalizedRating]]
  /** JSON-encoded copy of some of the rating's data
   * being kept in doctor's record for median rating recalculations.<br> 
   * problem -> ratingKey -> fieldName (actLevStart|actLevEnd) -> fieldValue */
  def stringToRatingsCopy (jsonRatings: String): RatingsCopy = {
    // https://sourceforge.net/projects/flexjson/forums/forum/686321/topic/3446279
    def cl = new flexjson.ClassLocator {
      val classLoader = Thread.currentThread.getContextClassLoader
      override def locate (map: java.util.Map[_,_], path: flexjson.Path): Class[_] = {
        if (map.containsKey ("actLevStart")) classOf[DenormalizedRating]
        else {
          val clazz = map.get ("class") // Flexjson class signature.
          if (clazz.isInstanceOf[String]) classLoader.loadClass (clazz.toString)
          else classOf[java.util.HashMap[_, _]]
        }
      }
    }
    if ((jsonRatings eq null) || jsonRatings.length == 0) new JHashMap
    else new flexjson.JSONDeserializer () .use (null, cl) .deserialize (jsonRatings)
  }
  def ratingsCopyToString (ratings: RatingsCopy): String = {
    new flexjson.JSONSerializer ()
      .exclude ("*.class") // Conserve space by not using Flexjson class signatures.
      .deepSerialize (ratings)
  }

  def calculateMedianRating (ratings: RatingsCopy, doctor: Entity): Unit = {
    def calcFor (problem: String): JDouble = if (!ratings.containsKey (problem)) null else {
      val deltas = ratings.get(problem) .values.map (_.activityRating) .toList
      deltas (deltas.size / 2) .toDouble
    }
    doctor.setProperty ("fmsRating", calcFor ("fms"))
    doctor.setProperty ("cfsRating", calcFor ("cfs"))
  }
}