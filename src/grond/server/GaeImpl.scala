package grond.server
import java.{util => ju}
import javax.servlet.http._
import com.google.appengine.api.users.{UserServiceFactory, User}

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
          reportErrorOrSuccess (Right (rating.id + ";" + doctorCreated))
        } catch {
          case UserException (message, kind) => reportErrorOrSuccess (Left ("Error " + kind + ": " + message))
          case ex => reportErrorOrSuccess (Left (ex.toString))
        }
      case op =>
        println ("Unknown op: " + op)
    }
  }

  override def doGet (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
  override def doPost (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
}
