package grond.model
import com.google.appengine.api.datastore.Entity

/** A doctor present in the GROND's database.
 * @param id The Datastore identifier of the doctor. */
case class Doctor (val id: String, var entity: Option[Entity] = None) {}

/** A rating present in the GROND's database.
 * @param id The Datastore identifier of the rating. */
case class Rating (val id: String, var entity: Option[Entity] = None) {}
object Rating {
  /** Whether setProperty or setUnindexedProperty is used for the given field. */
  def isIndexed (field: String) = false
}