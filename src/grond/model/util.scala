package grond.model

import java.{util => ju}
import com.google.appengine.api.datastore.{Query, Entity}
import com.google.appengine.api.datastore.FetchOptions.Builder._

object util {
  /** Used to check for parameters. */
  def isEmpty (str: String) = (str eq null) || str.length == 0
  /** Used to save space by storing empty strings as <code>null</code>. */
  def emptyToNull (str: String) = if (isEmpty (str)) null else str
  /** Fetches results from a query. */
  def queryToList (query: Query): ju.List[Entity] =
    Datastore.SERVICE.prepare (query) .asList (withChunkSize(100))
}