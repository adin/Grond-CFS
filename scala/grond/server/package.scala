package grond
import java.{util => ju}
import javax.servlet.http.HttpServletRequest
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.google.gwt.user.server.rpc.SerializationPolicyLoader
import com.googlecode.objectify.Key
import grond.shared.{Doctor, DoctorRating, UserException}

package object server {
  /** Retrieve either the real user or the mock user for the unit tests. */
  protected[server] def getUser (request: javax.servlet.http.HttpServletRequest): User = {
    val user = UserServiceFactory.getUserService.getCurrentUser()
    if (user ne null) return user

    val mockUserEmail = request.getHeader ("MockUserEmail")
    if ((mockUserEmail ne null) && (mockUserEmail endsWith "@test.test")) {
      val ua = request.getHeader ("User-Agent") // Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19 AppEngine-Google; (+http://code.google.com/appengine; appid: javagrond),gzip(gfe)
      val isLocal = request.getRemoteAddr == "127.0.0.1"
      if (isLocal || ((ua contains "AppEngine-Google") && (ua contains "appengine; appid: javagrond"))) {
        if (mockUserEmail.exists {case ch => ch != '@' && ch != '.' && !ch.isLetterOrDigit})
          throw new Exception ("Bad email: " + mockUserEmail)
        new User(mockUserEmail, "mock", mockUserEmail)
      } else null
    } else null
  }
  protected[server] def userId (user: User) =
    if (user.getFederatedIdentity ne null) user.getFederatedIdentity else user.getUserId
  protected[server] def userHash (userId: String) =
    {val crc32 = new java.util.zip.CRC32; crc32.update (userId getBytes "UTF-8"); crc32.getValue}

  // Security helper.
  protected[server] def ratingBelongsToUser (rating: DoctorRating, user: User): Boolean = {
    assert ((user ne null) && (rating ne null))
    assert ((rating.user ne null) && rating.user.length != 0)
    val federated = user.getFederatedIdentity
    if ((federated ne null) && federated.length != 0)
      federated == rating.user
    else
      user.getUserId == rating.user
  }

  /** See `DoctorRating.getFullId`. */
  protected[server] def ratingIdToKey (ratingId: String): Key[DoctorRating] = {
    assert ((ratingId ne null) && ratingId.length != 0)
    val dot = ratingId.indexOf ('.')
    if (dot < 1) throw new UserException ("No dot in ratingId", 1)
    val doctorId = java.lang.Long.parseLong (ratingId.substring (0, dot))
    val doctorKey = new Key (classOf[Doctor], doctorId)
    val ratingLId = java.lang.Long.parseLong (ratingId.substring (dot + 1))
    new Key (doctorKey, classOf[DoctorRating], ratingLId)
  }

  // NB: Fresh serialization policy is copied by /scala/makefile
  // from the GWT-generated /war/grond/*.gwt.rpc to /bin/gwt.rpc.
  // CF: http://www.techhui.com/profiles/blogs/simpler-and-speedier-gwt-with
  protected[server] lazy val GWT_SERIALIZATION_POLICY = {
    val cfe = new ju.LinkedList[ClassNotFoundException]
    val policy = SerializationPolicyLoader.loadFromStream (getClass.getResourceAsStream ("/gwt.rpc"), cfe)
    if (!cfe.isEmpty) println ("grond.server: SerializationPolicyLoader: " + cfe)
    policy
  }

  /** Used to pass request from `GaeImpl` to `ServerImpl`. */
  protected[server] val REQUEST = new scala.util.DynamicVariable[HttpServletRequest] (null)
}
