package grond.model
import java.{util => ju}
import javax.servlet.http.{HttpServletRequest, HttpSession}
import javax.jdo.PersistenceManager
import scala.collection.JavaConversions._
import com.google.appengine.api.users.{UserServiceFactory, User}
import com.google.appengine.api.datastore._
import grond.model.Datastore.implicits._
import grond.shared.Countries
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
      assert (KeyFactory.keyToString (userRatings.head.getKey) == rating.id)
      val doctorRatings = findRatings (doctor)
      assert (KeyFactory.keyToString (doctorRatings.head.getKey) == rating.id)
    } finally {
      remove (user)
      assert (findRatings (user) .isEmpty)

      if (outerDoctor ne null) {
        remove (outerDoctor)
        assert (queryToList (new Query (KeyFactory.stringToKey (outerDoctor.id))) .isEmpty)
      }
    }
  }

  /** This method's parameters uniquely identify one of doctor's ratings
   * (there can't be two doctors with the same name and location,
   * and there can't be two ratings of that doctor from the same user).
   * The method will create the doctor and the rating if they do not exist.<br>
   * Returns `true` if a new doctor entry was created. */
  def getRating (user: User,
    doctorFirstName: String, doctorLastName: String,
    country: String, region: String, city: String, problem: String): (Doctor, Rating, Boolean) = {

    // TODO: Additional checks for all parameters?
    // (Like name and surname starting with a capital and not being a single letter).

    if (user eq null) throw new UserException (
      "You are not signed in! You must sign in to submit the info.", 1)
    val datastore = Datastore.SERVICE

//    // See if the user asks to edit an existing rating.
//    val editRating = request.getParameter ("editRating"); if (editRating ne null) {
//      val key = KeyFactory.stringToKey (editRating)
//      val rating = datastore.get (key)
//
//      val ratingOpenID = rating ("userOpenID")
//      if (ratingOpenID != api.getUser.id) throw new UserException ("Owned by another user.", 5)
//
//      val sesIdKey = "rating id for " + rating ("lastName") + ", " + rating ("firstName")
//      session.setAttribute (sesIdKey, KeyFactory.keyToString (rating.getKey))
//      currentRating = VeloEntity (rating)
//      return pathInfo
//    }

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

    // Save doctor's data not used during Entity creation.
    rating.setUnindexedProperty ("problem", problem)

// That data isn't necessary for doctor's creation and should be updated in a separate method!
//    // Save data into the rating.
//    for (name <- "street" :: "phone" :: "email" :: "website" :: Nil) {
//      rating.setUnindexedProperty (name, api.trimmedParameter (name))
//    }

    rating.setProperty ("lastUpdate", new java.util.Date)

    // Save rating.
    Datastore.SERVICE.put (rating)
    (Doctor (KeyFactory.keyToString (doctor.getKey), Some (doctor)),
     Rating (KeyFactory.keyToString (rating.getKey), Some (rating)),
     doctorCreated)
  }

  def findRatings (user: User): ju.List[Entity] = {
    val query = new Query ("DoctorRating")
    val userId = if (user.getFederatedIdentity ne null) user.getFederatedIdentity else user.getUserId
    query.addFilter ("user", Query.FilterOperator.EQUAL, userId)
    queryToList (query)
  }

  def findRatings (doctor: Doctor): ju.List[Entity] = {
    queryToList (new Query ("DoctorRating", KeyFactory.stringToKey (doctor.id)))
  }

  def remove (user: User): Unit = {
    for (rating <- findRatings (user)) Datastore.SERVICE.delete (rating.getKey)
  }

  def remove (doctor: Doctor): Unit = {
    for (rating <- findRatings (doctor)) Datastore.SERVICE.delete (rating.getKey)
    Datastore.SERVICE.delete (KeyFactory.stringToKey (doctor.id))
  }

  def getDoctorsByRating (country: Countries.Country, region: String, condition: String, limit: Int): ju.List[Entity] = {
    val query = new Query ("Doctor")
    query.addFilter ("country", Query.FilterOperator.EQUAL, country.id)
    if (region != null && region.length != 0)
      query.addFilter ("region", Query.FilterOperator.EQUAL, region)
    val rating = condition + "Rating"
    query.addFilter (rating, Query.FilterOperator.NOT_EQUAL, "") // Only doctors rated for condition.
    query.addSort (rating, Query.SortDirection.DESCENDING)
    query.addSort ("firstName", Query.SortDirection.ASCENDING)
    import com.google.appengine.api.datastore.FetchOptions.Builder._
    Datastore.SERVICE.prepare (query) .asList (withLimit(limit))
  }
}

object doctorNameAndLocationUtility {
  /** Retrieve doctor Entity using doctor's name and location
   * or create a new one if not found.<br>
   * Returns `true` if a new doctor entry was created. */
  def newOrExistingDoctor (country: String, region: String, city: String,
  firstName: String, lastName: String): (Entity, Boolean) = {

    val query = new Query ("Doctor")
    // NB: In order for these equality filters to work the fields must use the automatic indexing (e.g. setProperty).
    //     Cf. http://code.google.com/intl/en/appengine/articles/index_building.html:
    //     "built-in indexes can handle" ... "equality filters on any number of properties".
    query.addFilter ("country", Query.FilterOperator.EQUAL, country)
    query.addFilter ("region", Query.FilterOperator.EQUAL, region)
    query.addFilter ("city", Query.FilterOperator.EQUAL, city)
    query.addFilter ("firstName", Query.FilterOperator.EQUAL, firstName)
    query.addFilter ("lastName", Query.FilterOperator.EQUAL, lastName)
    val doctor = Datastore.SERVICE.prepare (query) .asSingleEntity
    if (doctor ne null) (doctor, false) else {
      val doctor = new Entity ("Doctor")
      doctor.setProperty ("country", country)
      doctor.setProperty ("region", region)
      doctor.setProperty ("city", city)
      doctor.setProperty ("firstName", firstName)
      doctor.setProperty ("lastName", lastName)
      Datastore.SERVICE.put (doctor)
      println ("doctorNameAndLocation; new doctor: " + doctor)
      (doctor, true)
    }
  }
  /** Create new rating, but only once (one doctor rating per user). New rating's aren't persisted. */
  def newOrExistingRating (doctor: Entity, user: User): Entity = {
    // Should store userId instead of User (which just stores email):
    // http://groups.google.com/group/google-appengine-java/browse_thread/thread/9fe6e35c84fda0e2/53e7294aa57e9a31
    val userId = if (user.getFederatedIdentity ne null) user.getFederatedIdentity else user.getUserId

    def newRating = {
      val rating = new Entity ("DoctorRating", doctor.getKey)
      rating.setProperty ("country", doctor ("country"))
      rating.setProperty ("region", doctor ("region"))
      rating.setProperty ("city", doctor ("city"))
      rating.setProperty ("firstName", doctor ("firstName"))
      rating.setProperty ("lastName", doctor ("lastName"))
      rating.setProperty ("user", userId)
      if (user.getEmail ne null) rating.setProperty("userEmail", user.getEmail)
      rating
    }
    val query = new Query ("DoctorRating", doctor.getKey)
    query.addFilter ("user", Query.FilterOperator.EQUAL, userId)
    val rating = Datastore.SERVICE.prepare (query) .asSingleEntity
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
