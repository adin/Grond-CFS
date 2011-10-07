package grond.model;
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.repackaged.org.json.{JSONObject, JSONArray}
import grond.shared.Doctor

object suggestions {
  def getDoctorSuggestions (region: String, city: String): JSONObject = {
    val json = new JSONObject ()
    val query = OFY.query (classOf[Doctor])
    if (region ne null) query.filter ("region", region)
    if (city ne null) query.filter ("city", city)
    val doctors = query.toList

    val cities = doctors.map (_.city) .toSet
    val citiesArray = new JSONArray ()
    for (city <- cities.toSeq.sorted) {citiesArray.put (city)}
    json.put ("cities", citiesArray)

    val names = doctors.map {case doctor =>
      doctor.firstName + " " + doctor.lastName + ", " + doctor.city
    } .toSet
    val namesArray = new JSONArray ()
    for (name <- names.toSeq.sorted) {namesArray.put (name)}
    json.put ("names", namesArray)

    json
  }
}
