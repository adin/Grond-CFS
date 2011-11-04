package grond.server

import java.{util => ju}
import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConversions._
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import com.google.appengine.repackaged.org.json.JSONObject
import com.google.appengine.api.users.{User, UserServiceFactory}
import com.googlecode.objectify.Key
import grond.model.OFY
import grond.model.doctorNameAndLocation
import grond.shared.ServerIf
import grond.shared.{Doctor, DoctorRating, Countries, UserException}, Countries.Country

class ServerImpl extends RemoteServiceServlet with ServerIf {
  protected def request: HttpServletRequest = {
    Option (REQUEST.value) .getOrElse (getThreadLocalRequest)
  }

  // Extend the list of serializable classes.
  // https://groups.google.com/group/google-web-toolkit/browse_thread/thread/9eb513e449be3940/
  override def serializableWhiteList (swl: grond.shared.SerializableWhiteList) = {
    new grond.shared.SerializableWhiteList
  }

  @throws(classOf[UserException])
  override def getDoctorsByRating (country: String, region: String, condition: String, limit: Int): ju.LinkedList[Doctor] = {
    val countryObj = Countries.getCountry (country)
    val doctors = doctorNameAndLocation.getDoctorsByRating (
      country = countryObj, region = region, condition = condition, limit = limit + 1) .fetch()
    val user = getUser (request)
    val userDoctors = if (user eq null) Nil else doctorNameAndLocation.getDoctorsByUser (
      userId = userId (user), country = countryObj, region = region)
    lazy val userHash_ = userHash (userId (user))

    val ret = new ju.LinkedList[Doctor]
    for (doctor <- doctors.take (limit) .toSet ++ userDoctors) {
      lazy val ratingInfos = doctor.ratings.map (new JSONObject (_))
      val ratedForCondition = if (doctors.exists (_.id == doctor.id)) true else {
        ratingInfos.map (_ getString "condition") contains condition
      }

      // Tell GWT if the doctor has been rated by the current user.
      if (userDoctors contains doctor) {
        // Send the "finished" magic word to GWT if the current user's rating was finished.
        doctor._fromCurrentUser = if (
          ratingInfos.exists {case ji => if ((ji getString "userHash") == userHash_) ji getBoolean "finished" else false}
        ) "finished" else "_"
      }

      if (ratedForCondition) ret add doctor
    }
    if (doctors.size > limit) ret add null // Null Doctor at the end of the List means there are more doctors.
    ret
  }

  protected def isAdmin = UserServiceFactory.getUserService.isUserAdmin()

  @throws(classOf[UserException])
  override def nameAndLocation (countryId: String, region: String, city: String,
      name: String, surname: String, problem: String): DoctorRating = {
    val user = getUser (request)
    val (doctor, rating, doctorCreated) = doctorNameAndLocation.getRating (
      user, name, surname, countryId, region, city, problem)
    if (!isAdmin && !ratingBelongsToUser (rating, user)) throw new UserException ("Security check failed.", 101)
    rating._doctor = doctor
    rating._doctorCreated = doctorCreated
    rating
  }

  @throws(classOf[UserException])
  override def getRating (ratingId: String): DoctorRating = {
    val ratingKey = ratingIdToKey (ratingId)
    val rating = OFY.get[DoctorRating] (ratingKey)
    rating._doctor = OFY.get[Doctor] (rating.doctor)
    val user = getUser (request)
    if (!isAdmin && !ratingBelongsToUser (rating, user)) throw new UserException ("Security check failed.", 101)
    rating
  }

  @throws(classOf[UserException])
  override def getDoctorTRP (doctorId: Long, needDoctorInfo: Boolean): Doctor = {
    val doctorKey = new Key[Doctor] (classOf[Doctor], doctorId)
    val doctor = OFY.get[Doctor] (doctorKey)
    if (needDoctorInfo) doctor._trpInfo = grond.model.doctorUtil.getTRPInfo (doctor)
    doctor
  }
}
