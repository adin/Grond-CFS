package grond.server
import java.{util => ju}
import javax.servlet.http._
import scala.collection.JavaConversions._
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.google.appengine.repackaged.org.json.JSONObject
import grond.model.{Rating, Doctor}

/**
 * Main server interface (JSON and String responses which used from GWT).
 */
class GaeImpl extends HttpServlet {
  /** Retrieve either the real user or the mock user for the unit tests. */
  def getUser (request: javax.servlet.http.HttpServletRequest): User = {
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

  /** Servlet's main method. */
  protected def welcome (request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType ("text/javascript")
    response.setCharacterEncoding ("utf-8")

    val callback = request.getParameter ("callback")
    if (callback eq null) return

    def respond (str: String): Unit = {
      val writer = response.getWriter
      writer.write (callback)

      // GAE http://www.json.org/javadoc/org/json/JSONObject.html
      writer.write (" (")
      writer.write (com.google.appengine.repackaged.org.json.JSONObject.quote (str))
      writer.write (");")
    }

    def reportErrorOrSuccess (rep: Either[String, String]): Unit = {
      // http://www.json.org/javadoc/org/json/JSONArray.html
      val json = new com.google.appengine.repackaged.org.json.JSONArray
      rep match {
        case Left (error) => json.put (error) .put ("")
        case Right (okay) => json.put ("") .put (okay)
      }
      respond (json.toString())
    }

    request.getParameter ("op") match {
      case "getCurrentUser" =>
        val user = getUser (request)
        if (user ne null) {
          val json = new com.google.appengine.repackaged.org.json.JSONObject
          json.put ("email", user.getEmail())
          json.put ("authDomain", user.getAuthDomain())
          json.put ("userId", user.getUserId())
          json.put ("federatedIdentity", user.getFederatedIdentity())
          if (user.getAuthDomain() == "mock") json.put ("isAdmin", false)
          else json.put ("isAdmin", UserServiceFactory.getUserService().isUserAdmin())
          respond (json.toString)
        } else respond ("")
      case "createLoginURL" =>
        val destinationURL = request.getParameter ("destinationURL")
        val authDomain = request.getParameter ("authDomain")
        val federatedIdentity = request.getParameter ("federatedIdentity")
        val loginUrl = UserServiceFactory.getUserService.createLoginURL (
          destinationURL, authDomain, federatedIdentity, new ju.HashSet[String])
        respond (loginUrl)
      case "createLogoutURL" =>
        val destinationURL = request.getParameter ("destinationURL")
        val logoutUrl = UserServiceFactory.getUserService.createLogoutURL (destinationURL)
        respond (logoutUrl)
      case "nameAndLocation" =>
        val user = getUser (request)
        val countryId = request.getParameter ("countryId")
        val region = request.getParameter ("region")
        val city = request.getParameter ("city")
        val name = request.getParameter ("name")
        val surname = request.getParameter ("surname")
        val problem = request.getParameter ("problem")
        import grond.model._
        try {
          val (doctor, rating, doctorCreated) = doctorNameAndLocation.getRating (
            user, name, surname, countryId, region, city, problem)
          val json = ratingToJson (rating, Some (doctor))
          json.put ("doctorCreated", doctorCreated)
          respond (json.toString)
        } catch {
          case UserException (message, kind) =>
            val json = new JSONObject
            json.put ("errorMessage", "Error " + kind + ": " + message)
            respond (json.toString)
          case ex =>
            val json = new JSONObject
            json.put ("errorMessage", ex.toString)
            respond (json.toString)
        }
      case "getRating" =>
        val ratingId = request.getParameter ("ratingId")
        assert ((ratingId ne null) && ratingId.length != 0)
        respond (ratingToJson (Rating (ratingId), None) .toString)
      case "nop" =>
      case op =>
        println ("Unknown op: " + op)
    }
  }

  protected def ratingToJson (rating: Rating, doctor: Option[Doctor]): JSONObject = {
    val ratingId: String = rating.id
    lazy val ratingKey = com.google.appengine.api.datastore.KeyFactory.stringToKey (ratingId)
    val ratingEntity = if (rating.entity.isDefined) rating.entity.get else {
      grond.model.Datastore.SERVICE.get (ratingKey)
    }
    val json = new JSONObject
    for ((key, value) <- ratingEntity.getProperties) {json.put (key, value)}
    val doctorEntity = if (doctor.isDefined && doctor.get.entity.isDefined) doctor.get.entity.get else {
      grond.model.Datastore.SERVICE.get (ratingKey.getParent)
    }
    val djson = new JSONObject
    for ((key, value) <- doctorEntity.getProperties) {djson.put (key, value)}
    json.put ("doctor", djson)
    json.put ("ratingId", ratingId)
    json
  }

  override def doGet (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
  override def doPost (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
}
