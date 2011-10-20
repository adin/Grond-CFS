package grond.server
import java.{util => ju}
import javax.servlet.http._
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore.{Entity, KeyFactory}
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.google.appengine.repackaged.org.json.{JSONObject, JSONArray}
import com.google.gwt.user.server.rpc.RPC
import com.googlecode.objectify.Key
import grond.model.{OFY, doctorUtil, doctorNameAndLocation, suggestions}
import grond.shared.{Countries, Doctor, DoctorRating, UserException}

/**
 * Main server interface (JSON and String responses which used from GWT).
 */
class GaeImpl extends HttpServlet {
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
    lazy val server = new ServerImpl

    request.getParameter ("op") match {
      case "getCurrentUser" =>
        if (user ne null) {
          val json = new JSONObject
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
        val method = classOf[ServerImpl].getMethod ("nameAndLocation",
          classOf[String], classOf[String], classOf[String], classOf[String], classOf[String], classOf[String])
        try {
          assert (method ne null)
          val rating = REQUEST.withValue (request) {server.nameAndLocation(
            countryId, region, city, name, surname, problem
          )}
          val encRating = RPC.encodeResponseForSuccess (method, rating, GWT_SERIALIZATION_POLICY)
          respond (encRating)
        } catch {
          case uex: UserException =>
            uex.printStackTrace()
            respond (RPC.encodeResponseForFailure (method, uex, GWT_SERIALIZATION_POLICY))
          case ex =>
            ex.printStackTrace()
            val uex = new UserException (ex.toString(), 0)
            respond (RPC.encodeResponseForFailure (method, uex, GWT_SERIALIZATION_POLICY))
        }
      case "getRating" =>
        val ratingId = request getParameter "ratingId"
        // XXX
      case "ratingUpdateList" =>
        val ratingId = request getParameter "ratingId"
        val ratingKey = ratingIdToKey (ratingId)
        val rating = OFY.get[DoctorRating] (ratingKey)
        if (!isAdmin && !ratingBelongsToUser (rating, user)) throw new UserException ("Security check failed.", 101)
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        val value = request getParameter "value"
        val vop = request getParameter "vop"
        var values = rating.getField (field) .asInstanceOf[ju.ArrayList[String]]
        if (values eq null) values = new ju.ArrayList[String]
        def update(): Unit = {
          rating.setField (field, values)
          val afterSave = ratingPostprocess (rating, field, values)
          OFY.put[DoctorRating] (rating)
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
        val ratingKey = ratingIdToKey (ratingId)
        val rating = OFY.get[DoctorRating] (ratingKey)
        if (!isAdmin && !ratingBelongsToUser (rating, user)) throw new UserException ("Security check failed.", 101)
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        val value = request getParameter "value"
        rating.setField (field, value)
        OFY.put[DoctorRating] (rating)
        respond ("")
      case "ratingRemove" =>
        val ratingId = request getParameter "ratingId"
        val ratingKey = ratingIdToKey (ratingId)
        val rating = OFY.get[DoctorRating] (ratingKey)
        if (!isAdmin && !ratingBelongsToUser (rating, user)) throw new UserException ("Security check failed.", 101)
        val field = request getParameter "field"
        assert (ratingOuterField (field), field)
        rating.setField (field, null)
        OFY.put[DoctorRating] (rating)
        respond ("")
      case "getDoctorsByRating" =>
        val doctors = REQUEST.withValue (request) {server.getDoctorsByRating(
          country = request getParameter "country",
          region = request getParameter "region",
          condition = request getParameter "condition",
          limit = Integer.parseInt (request getParameter "limit")
        )}
        val method = classOf[ServerImpl].getMethod ("getDoctorsByRating",
          classOf[String], classOf[String], classOf[String], Integer.TYPE)
        val encoded = RPC.encodeResponseForSuccess (method, doctors, GWT_SERIALIZATION_POLICY)
        respond (encoded)
      case "nop" =>
      case "getDoctorTRP" =>
        val doctorId = (request getParameter "doctorId").toLong
        val doctorKey = KeyFactory.createKey ("Doctor", doctorId)
//        lazy val doctor = Datastore.SERVICE.get (doctorKey)
//
//        val json = doctorUtil.getTRPInfo (doctorKey)
//
//        val needDoctorInfo = (request getParameter "needDoctorInfo") == "true"
//        if (needDoctorInfo) json.put ("doctor", doctorToJson (doctor, user) ._1)
//
//        respond (json.toString)
      case "getDoctorSuggestions" =>
        respond (suggestions.getDoctorSuggestions (request getParameter "region", request getParameter "city") .toString)
      case op =>
        println ("Unknown op: " + op)
    }
  }

  /** Do any additional actions if necessary when a rating field is updated.<br>
   * Note: Rating is saved by the caller after this method returns.<br>
   * Returns a function which should be called after the rating is saved. */
  protected def ratingPostprocess (rating: DoctorRating, field: String, oldValue: AnyRef): Option[()=>Unit] =
    field match {
      case "type" | "averageCost" | "experience" | "satAfter" =>
        Some (() => doctorUtil.updateFromRatings (OFY.get[Doctor] (rating.doctor)))
      case _ => None
    }

  /** Checks if the field is allowed to be updated directly with RPC requests. */
  protected def ratingOuterField (field: String): Boolean = {
    field != "fmRating" && field != "cfsRating"
  }

  override def doGet (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
  override def doPost (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
}
