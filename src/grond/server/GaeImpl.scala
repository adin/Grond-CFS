package grond.server
import java.{util => ju}
import javax.servlet.http._
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore.{Entity, KeyFactory}
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.google.appengine.repackaged.org.json.{JSONObject, JSONArray}
import grond.model.{Rating, Doctor, Datastore, doctorUtil, doctorNameAndLocation, UserException}
import grond.shared.Countries

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
    response setContentType "text/javascript"
    response setCharacterEncoding "utf-8"

    val callback = request getParameter "callback"
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

    lazy val user = getUser (request)
    lazy val isAdmin = UserServiceFactory.getUserService.isUserAdmin()

    request.getParameter ("op") match {
      case "getCurrentUser" =>
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
        val destinationURL = request getParameter "destinationURL"
        val authDomain = request getParameter "authDomain"
        val federatedIdentity = request.getParameter ("federatedIdentity")
        val loginUrl = UserServiceFactory.getUserService.createLoginURL (
          destinationURL, authDomain, federatedIdentity, new ju.HashSet[String])
        respond (loginUrl)
      case "createLogoutURL" =>
        val destinationURL = request getParameter "destinationURL"
        val logoutUrl = UserServiceFactory.getUserService.createLogoutURL (destinationURL)
        respond (logoutUrl)
      case "nameAndLocation" =>
        val countryId = request getParameter "countryId"
        val region = request getParameter "region"
        val city = request getParameter "city"
        val name = request getParameter "name"
        val surname = request getParameter "surname"
        val problem = request getParameter "problem"
        try {
          val (doctor, rating, doctorCreated) = doctorNameAndLocation.getRating (
            user, name, surname, countryId, region, city, problem)
          val json = ratingToJson (rating, Some (doctor), user, checkAuth = true)
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
        val ratingId = request getParameter "ratingId"
        assert ((ratingId ne null) && ratingId.length != 0)
        respond (ratingToJson (Rating (ratingId), None, user, checkAuth = !isAdmin) .toString)
      case "ratingUpdateList" =>
        val ratingId = request getParameter "ratingId"
        val ratingKey = KeyFactory.stringToKey (ratingId)
        val rating = Datastore.SERVICE.get (ratingKey)
        if (!ratingBelongsToUser (rating, user) && !isAdmin) throw new Exception ("Security check failed.")
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        val value = request getParameter "value"
        val vop = request getParameter "vop"
        var values = rating.getProperty (field) .asInstanceOf[ju.List[String]]
        if (values eq null) values = new ju.LinkedList[String]
        def update(): Unit = {
          if (Rating.isIndexed (field)) rating.setProperty (field, values)
          else rating.setUnindexedProperty (field, values)
          val afterSave = ratingPostprocess (rating, field, values)
          Datastore.SERVICE.put (rating)
          if (afterSave.isDefined) afterSave.get.apply
        }
        vop match {
          case "add" =>
            if (!values.contains (value)) {
              values.add (value)
              update()
            }
          case "remove" =>
            if (values contains value) {
              val removed = values.remove (value)
              assert (removed)
              update()
            }
          case _ => throw new Exception ("Unknown vop: " + vop)
        }
        respond ("")
      case "ratingUpdateString" =>
        val ratingId = request getParameter "ratingId"
        val ratingKey = KeyFactory.stringToKey (ratingId)
        val rating = Datastore.SERVICE.get (ratingKey)
        if (!ratingBelongsToUser (rating, user) && !isAdmin) throw new Exception ("Security check failed.")
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        val value = request getParameter "value"
        val have = rating.getProperty(field).asInstanceOf[String]
        if (value != have) {
          if (Rating.isIndexed (field)) rating.setProperty (field, value)
          else rating.setUnindexedProperty (field, value)
          val afterSave = ratingPostprocess (rating, field, have)
          Datastore.SERVICE.put (rating)
          if (afterSave.isDefined) afterSave.get.apply
        }
        respond ("")
      case "ratingRemove" =>
        val ratingId = request getParameter "ratingId"
        val ratingKey = KeyFactory.stringToKey (ratingId)
        val rating = Datastore.SERVICE.get (ratingKey)
        if (!ratingBelongsToUser (rating, user) && !isAdmin) throw new Exception ("Security check failed.")
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        if (rating.hasProperty (field)) {
          val have = rating.getProperty (field)
          rating.removeProperty (field)
          val afterSave = ratingPostprocess (rating, field, have)
          Datastore.SERVICE.put (rating)
          if (afterSave.isDefined) afterSave.get.apply
        }
        respond ("")
      case "getDoctorsByRating" =>
        val doctors = new JSONArray
        val realLimit = Integer.parseInt (request getParameter "limit")
        val entities = doctorNameAndLocation.getDoctorsByRating (
          country = Countries.getCountry (request getParameter "country"),
          region = request getParameter "region",
          condition = request getParameter "condition",
          limit = realLimit + 1)
        for (doctor <- entities.take (realLimit)) {
          val json = new JSONObject
          for ((key, value) <- doctor.getProperties) {json.put (key, value)}
          doctors.put (doctors.length(), json)
        }
        if (entities.size() > realLimit) doctors.put (doctors.length(), "There's more.")
        respond (doctors.toString)
      case "nop" =>
      case op =>
        println ("Unknown op: " + op)
    }
  }

  /** Do any additional actions if necessary when a rating field is updated.<br>
   * Note: Rating is saved by the caller after this method returns.<br>
   * Returns a function which should be called after the rating is saved. */
  protected def ratingPostprocess (rating: Entity, field: String, oldValue: AnyRef): Option[()=>Unit] = field match {
    case "type" | "averageCost" | "experience" | "satAfter" =>
      Some (() => doctorUtil.updateFromRatings (rating.getKey.getParent))
    case _ => None
  }

  /** Checks if the field is allowed to be updated directly with RPC requests. */
  protected def ratingOuterField (field: String): Boolean = {
    field != "fmRating" && field != "cfsRating"
  }

  protected def ratingBelongsToUser (rating: Entity, user: User): Boolean = {
    assert ((user ne null) && (rating ne null))
    val userId = rating.getProperty ("user") .asInstanceOf[String]
    assert ((userId ne null) && userId.length != 0)
    val federated = user.getFederatedIdentity
    if ((federated ne null) && federated.length != 0)
      federated == userId
    else
      user.getUserId == userId
  }

  protected def ratingToJson (rating: Rating, doctor: Option[Doctor], user: User, checkAuth: Boolean): JSONObject = {
    val ratingId: String = rating.id
    lazy val ratingKey = KeyFactory.stringToKey (ratingId)
    val ratingEntity = if (rating.entity.isDefined) rating.entity.get else {
      Datastore.SERVICE.get (ratingKey)
    }
    if (checkAuth && !ratingBelongsToUser (ratingEntity, user)) throw new Exception ("Security check failed.")
    val json = new JSONObject
    for ((key, value) <- ratingEntity.getProperties) {json.put (key, value)}
    val doctorEntity = if (doctor.isDefined && doctor.get.entity.isDefined) doctor.get.entity.get else {
      Datastore.SERVICE.get (ratingKey.getParent)
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
