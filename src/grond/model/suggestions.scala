package grond.model;
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.repackaged.org.json.{JSONObject, JSONArray}
import com.google.appengine.api.datastore._

object suggestions {
  def getDoctorSuggestions (region: String, city: String): JSONObject = {
    val json = new JSONObject ()
    val query = new Query ("Doctor")
    if (region ne null) query.addFilter ("region", Query.FilterOperator.EQUAL, region)
    if (city ne null) query.addFilter ("city", Query.FilterOperator.EQUAL, city)
    val doctors = util.queryToList (query)

    val cities = doctors.map (_.getProperty ("city") .asInstanceOf[String]) .toSet
    val citiesArray = new JSONArray ()
    for (city <- cities.toSeq.sorted) {citiesArray.put (city)}
    json.put ("cities", citiesArray)

    val names = doctors.map {case entity =>
      entity.getProperty ("firstName") .asInstanceOf[String] + " " +
      entity.getProperty ("lastName") .asInstanceOf[String] + ", " +
      entity.getProperty ("city") .asInstanceOf[String]
    } .toSet
    val namesArray = new JSONArray ()
    for (name <- names.toSeq.sorted) {namesArray.put (name)}
    json.put ("names", namesArray)

    json
  }
}
