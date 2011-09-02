package grond.model;
import java.{util => ju}
import scala.collection.mutable, scala.collection.JavaConversions._
import com.google.appengine.repackaged.org.json.{JSONArray}
import com.google.appengine.api.datastore._

object suggestions {
  def getCitySuggestions (region: String): JSONArray = {
    val array = new JSONArray ()
    val query = new Query ("Doctor")
    if (region ne null) query.addFilter ("region", Query.FilterOperator.EQUAL, region)
    val cities = util.queryToList (query) .map (_.getProperty ("city") .asInstanceOf[String]) .toSet
    for (city <- cities.toSeq.sorted) {array.put (city)}
    array
  }
}
