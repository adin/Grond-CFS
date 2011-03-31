package grond.model
import com.google.appengine.api.datastore._

object Datastore {
  val SERVICE = DatastoreServiceFactory.getDatastoreService

  object implicits {
    implicit def entityAsFunctionToProperties (entity: Entity) =
      (name: String) => entity.getProperty (name)
  }
}
