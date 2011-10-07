package grond
import java.{util => ju}
import com.googlecode.objectify.{ObjectifyService, ObjectifyFactory, ObjectifyOpts, Objectify}
import grond.shared.{Doctor, DoctorRating}

package object model {
  ObjectifyService.register (classOf[Doctor])
  ObjectifyService.register (classOf[DoctorRating])
  def OFY = ObjectifyService.begin()

  /** Used to check for parameters. */
  def isEmpty (str: String) = (str eq null) || str.length == 0
  /** Used to save space by storing empty strings as <code>null</code>. */
  def emptyToNull (str: String) = if (isEmpty (str)) null else str
}
