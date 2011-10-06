package grond.model
import java.{util => ju}
import javax.servlet.http.{HttpServletRequest, HttpSession}
import javax.jdo.PersistenceManager
import scala.collection.JavaConversions._
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.googlecode.objectify.{Query, Key}
import grond.shared.{Countries, Doctor, DoctorRating}
import grond.htmlunit.fun.randomAlphabeticalString

/**
 * This object helps finding or creating doctor's ratings.<br>
 * The doctor entity is automatically created as required.<br>
 */
object doctorNameAndLocation { import doctorNameAndLocationUtility._, util._
  def _test: Unit = {
    val random = new java.util.Random
    def randString = randomAlphabeticalString (random)
    val user = new User (randString, randString, randString, randString)
    var outerDoctor: Doctor = null
    try {
      val (doctor, rating, isNew) = getRating (user,
        doctorFirstName = randString, doctorLastName = randString,
        country = "testCountry", region = randString, city = randString,
        problem = if (random.nextBoolean) "cfs" else "fm")
      outerDoctor = doctor
      val userRatings = findRatings (user)
      assert (userRatings.head.id == rating.id)
      val doctorRatings = findRatings (doctor)
      assert (doctorRatings.head.id == rating.id)
    } finally {
      remove (user)
      assert (findRatings (user) .fetchKeys() .isEmpty)

      if (outerDoctor ne null)
        assert (OFY.find (classOf[Doctor], outerDoctor.id) == null)
    }
  }

  /** This method's parameters uniquely identify one of doctor's ratings
   * (there can't be two doctors with the same name and location,
   * and there can't be two ratings of that doctor from the same user).
   * The method will create the doctor and the rating if they do not exist.<br>
   * Returns `true` if a new doctor entry was created. */
  def getRating (user: User,
    doctorFirstName: String, doctorLastName: String,
    country: String, region: String, city: String, problem: String): (Doctor, DoctorRating, Boolean) = {

    if (user eq null) throw new UserException (
      "You are not signed in! You must sign in to submit the info.", 1)

    val firstName = doctorFirstName.trim
    val lastName = doctorLastName.trim
    if (isEmpty (firstName) || isEmpty (lastName)) throw new UserException (
      "You haven't filled the doctor's name.", 2)

    if (!firstName.matches("^\\w+.*") || !lastName.matches("^\\w+.*")) throw new UserException (
      "Doctor name seems to be wrong.", 2)
    if (firstName.matches("^\\d+") || lastName.matches("^\\d+")) throw new UserException (
      "Doctor name seems to be wrong.", 2)

    val tCountry = country.trim
    if (isEmpty (tCountry)) throw new UserException (
      "You haven't filled the country.", 3)
    val ttCountry = grond.shared.Countries.COUNTRIES.find (_.id == tCountry)
    if (ttCountry.isEmpty && tCountry != "testCountry") throw new UserException (
      "Internal error: Unknown country name!", 3)

    val tRegion = region.trim; if (isEmpty (tRegion)) throw new UserException (
      "You haven't filled the region.", 3)
    val tCity = city.trim; if (isEmpty (tRegion)) throw new UserException (
      "You haven't filled the city.", 3)
    val tProblem = problem; if (isEmpty (tProblem)) throw new UserException (
      "You haven't selected the diagnosis.", 3)

    val (doctor, doctorCreated) = newOrExistingDoctor (country, region, city, firstName, lastName)
    val rating = newOrExistingRating (doctor, user)

    rating.problem = problem

    rating.lastUpdate = new java.util.Date

    // Save rating.
    OFY.async().put[DoctorRating] (rating)
    (doctor, rating, doctorCreated)
  }

  def findRatings (user: User): Query[DoctorRating] = {
    val userId = if (user.getFederatedIdentity ne null) user.getFederatedIdentity else user.getUserId
    OFY.query (classOf[DoctorRating]) .filter ("user", userId)
  }

  def findRatings (doctor: Doctor): Query[DoctorRating] = {
    OFY.query (classOf[DoctorRating]) .ancestor(doctor)
  }

  def remove (user: User): Unit = {
    OFY.delete (findRatings (user))
  }

  def remove (doctor: Doctor): Unit = {
    OFY.delete (findRatings (doctor))
    OFY.delete (doctor)
  }

  def getDoctorsByRating (country: Countries.Country, region: String, condition: String, limit: Int): Query[Doctor] = {
    val query = OFY.query (classOf[Doctor])
    query.filter ("country", country.id)
    if (region != null && region.length != 0) query.filter ("region", region)
    val rating = "_" + condition + "Satisfaction"
    query.filter (rating + " !=", "") // Only doctors rated for condition.
    query.order ("-" + rating)
    query.order ("firstName")
    query.limit (limit)
    query
  }

  def getDoctorsByUser (userId: String, country: Countries.Country, region: String): Iterable[Doctor] = {
    val query = OFY.query (classOf[DoctorRating])
    query.filter ("user", userId)
    if (country != null) query.filter ("country", country.id)
    if (region != null && region.length != 0) query.filter ("region", region)
    query.fetchParents[Doctor]().values()
  }
}

object doctorNameAndLocationUtility {
  /** Retrieve doctor Entity using doctor's name and location
   * or create a new one if not found.<br>
   * Returns `true` if a new doctor entry was created. */
  def newOrExistingDoctor (country: String, region: String, city: String,
  firstName: String, lastName: String): (Doctor, Boolean) = {

    val query = OFY.query (classOf[Doctor])
    // NB: In order for these equality filters to work the fields must use the automatic indexing (e.g. setProperty).
    //     Cf. http://code.google.com/intl/en/appengine/articles/index_building.html:
    //     "built-in indexes can handle" ... "equality filters on any number of properties".
    query.filter ("country", country)
    query.filter ("region", region)
    query.filter ("city", city)
    query.filter ("firstName", firstName)
    query.filter ("lastName", lastName)
    val doctor = query.get()
    if (doctor ne null) (doctor, false) else {
      val doctor = new Doctor
      doctor.country = country;
      doctor.region = region;
      doctor.city = city;
      doctor.firstName = firstName;
      doctor.lastName = lastName;
      OFY.put (classOf[Doctor], doctor)
      println ("doctorNameAndLocation; new doctor: " + doctor)
      (doctor, true)
    }
  }
  /** Create new rating, but only once (one doctor rating per user). New rating's aren't persisted. */
  def newOrExistingRating (doctor: Doctor, user: User): DoctorRating = {
    // Should store userId instead of User (which just stores email):
    // http://groups.google.com/group/google-appengine-java/browse_thread/thread/9fe6e35c84fda0e2/53e7294aa57e9a31
    val userId = if (user.getFederatedIdentity ne null) user.getFederatedIdentity else user.getUserId

    def newRating = {
      val rating = new DoctorRating
      rating.doctor = new Key (classOf[Doctor], doctor.id)
      rating.country = doctor.country
      rating.region = doctor.region
      rating.city = doctor.city
      rating.firstName = doctor.firstName
      rating.lastName = doctor.lastName
      rating.user = userId
      if (user.getEmail ne null) rating.userEmail = user.getEmail
      rating
    }
    val query = OFY.query (classOf[DoctorRating]) .ancestor (doctor)
    query.filter ("user", userId)
    val rating = query.get()
    if (rating ne null) rating else newRating
  }
  /** "doctorFirstName" and "doctorLastName" fields should be preserved thru the forms
   * in order to identify the rating id in the session. */
//  def continueWithTheCurrentRating (api: GrondAPI): Entity = {
//    // Obtain the existing rating (using the doctor's name and the session).
//    val firstName = api.trimmedParameter ("doctorFirstName")
//    val lastName = api.trimmedParameter ("doctorLastName")
//    if ((firstName eq null) || (lastName eq null)) throw new UserException ("Doctor's name lost!", 2)
//    val session = api.getRequest.getSession
//    val sesIdKey = "rating id for " + lastName + ", " + firstName
//    var ratingKey = session.getAttribute (sesIdKey) .asInstanceOf[String]
//    if (ratingKey eq null) throw new UserException ("Rating id lost! Go back to the first page.", 3)
//    Datastore.SERVICE.get (KeyFactory.stringToKey (ratingKey))
//  }
}
