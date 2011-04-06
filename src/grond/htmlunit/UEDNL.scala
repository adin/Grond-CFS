package grond.htmlunit

import scala.collection.mutable
import scala.collection.JavaConversions._

import com.google.appengine.api.datastore.{Query, Entity}, Query.FilterOperator.EQUAL
import com.google.appengine.api.datastore.FetchOptions.Builder._

import com.gargoylesoftware.htmlunit.{WebClient}
import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage, HtmlTable, HtmlAnchor,
  HtmlObject, HtmlOption, HtmlInput, HtmlDivision, HtmlButton}
import com.gargoylesoftware.htmlunit.util.Cookie

import grond.model.Datastore

import grond.htmlunit.fun.randomAlphabeticalString

/**
 * Clicks on the "add doctor" button and enters the doctor's location.
 */
class UEDNL (webClient: WebClient, hostUrl: String) extends Test (webClient, hostUrl) {
  val TEMPORARY_DOCTORS = new mutable.ListBuffer[(String, String, String, String)]
  override def run: Unit = try {
    var page = signOut (None)
    navigateToUsaCfs (page)

    // Test: Navigating to the 'new doctor' form requires login.
    // Test outline:
    //   Open main page. Wait.
    //   Click on United States, CFS. Wait.
    //   Find the button which adds a doctor. Click it. Wait.
    //   See request to login. *Login*.

    // Make sure the basic RPC works before clicking any RPC-dependent buttons.
    assert (page.asText contains "GROND version ", "RPC not functional!")

    // Find and click the button to add a new doctor.
    // $x("//button[contains(text(),'doctor') and contains(text(),'add')]") in Firebug console.
    val newDoctorButtonPath = "//button[contains(text(),'doctor') and contains(text(),'add')]"
    val newDoctorButton = getButton (page, newDoctorButtonPath)
    assert (newDoctorButton ne null, "The button to add a new doctor hasn't been found.")
    newDoctorButton.click

    // Find and click the login button.
    assert (getButton (page, newDoctorButtonPath) eq null) // The button has gone.
    assert (getDiv (page, "//div[contains(text(), 'Not signed in! Please sign in to add the doctor.')]") ne null)
    val googleLoginPath = "//button[text()='Sign in with Google']"
    val googleLogin = getButton (page, googleLoginPath)
    assert (googleLogin ne null)
    val openidLoginPath = "//button[text()='Sign in']"
    assert (getButton (page, openidLoginPath) ne null)
    page = googleLogin.click()

    page = signIn2 (page, "firstTestUser@test.test", "#mapOf_usa_cfs")
    getButton (page, newDoctorButtonPath) .click

    // Test: 'New doctor' form has all expected elements in place.
    // Test outline:
    //   Check that there is a message describing what the page does and why.
    //   Check that there is a map of United States present.
    //   See if there is also a textual selector of regions (e.g. states).
    //   Enter a random (unexisting) city name.
    //   Enter a random (unexisting) doctor name.
    //   Enter a random (unexisting) doctor surname.
    //   Check that the "next" button is now enabled.
    //   Click "next". Wait. See map and name-location form disappear,
    //   replaced by a "thank you" message for entering a new doctor into the database
    //   and a prompt to continue with the rating.

    // Still logged in?
    assert (page.asText contains "Hi, you are signed in as firstTestUser@test.test!", page.asText)

    assert (getDiv (page, "//div[contains(text(), 'Thanks for participating!')]") ne null, page.asXml)
    assert (getDiv (page, "//div[contains(text(), 'Please enter your doctor')]") ne null)

    val ammap = page.getFirstByXPath ("//object[@id='ammap']") .asInstanceOf[HtmlObject]
    assert (ammap ne null, page.asXml)
    assert (ammap.asXml contains "countries/usa/ammap_settings", ammap.asXml)

    val regionWyoming = page.getFirstByXPath ("//select/option[@value='Wyoming']") .asInstanceOf[HtmlOption]
    assert (regionWyoming ne null, page.asXml)

    val random = new java.util.Random
    var city: HtmlInput = null; var name: HtmlInput = null; var surname: HtmlInput = null
    var error: HtmlDivision = null; var next: HtmlButton = null
    def getForm(): Unit = {
      city = page.getFirstByXPath ("//input[@id='dnl-city']") .asInstanceOf[HtmlInput]
      name = page.getFirstByXPath ("//input[@id='dnl-name']") .asInstanceOf[HtmlInput]
      surname = page.getFirstByXPath ("//input[@id='dnl-surname']") .asInstanceOf[HtmlInput]
      error = getDiv (page, "//div[@id='dnl-error']"); assert (error ne null)
      next = getButton (page, "//button[@id='dnl-next']")
    }
    getForm()

    assert (next.isDisabled, next.asXml)

    // Try numeric values.
    val city1 = random.nextLong.toString
    city.setValueAttribute (city1)
    val name1 = random.nextLong.toString
    name.setValueAttribute (name1)
    val surname1 = random.nextLong.toString
    surname.setValueAttribute (surname1)
    TEMPORARY_DOCTORS += (("Alabama", city1, name1, surname1))
    assert (!next.isDisabled, next.asXml)
    next.click()
    // Error from the use of numeric values.
    assert (error.getTextContent contains "Doctor name seems to be wrong")

    val city2 = randomAlphabeticalString (random)
    city.setValueAttribute (city2)
    val name2 = "_" + randomAlphabeticalString (random) // Prefix "_" makes the doctor invisible to normal users.
    name.setValueAttribute (name2)
    val surname2 = "_" + randomAlphabeticalString (random)
    surname.setValueAttribute (surname2)
    TEMPORARY_DOCTORS += (("Alabama", city2, name2, surname2))
    next.click()

    assert (page.getFirstByXPath ("//object[@id='ammap']") == null, error.getTextContent + "\n\n" + page.asXml)
    assert (page.getFirstByXPath ("//input[@id='dnl-name']") == null)
    assert (getDiv (page, "//div[contains(text()," +
      " 'Thanks for telling us about this practitioner!')]") ne null, page.asText)
    assert (getDiv (page, "//div[contains(text(), '" + name2 + "')]") ne null, page.asText) // We see whose rating we edit.
    assert (getDiv (page, "//div[contains(text(), 'TYPE OF HEALTH PROFESSIONAL')]") ne null, page.asText)

    val query = new Query ("Doctor")
    query.addFilter ("country", EQUAL, "usa")
    query.addFilter ("region", EQUAL, "Alabama")
    query.addFilter ("city", EQUAL, city2)
    query.addFilter ("firstName", EQUAL, name2)
    query.addFilter ("lastName", EQUAL, surname2)
    val count1 = Datastore.SERVICE.prepare (query) .countEntities(withChunkSize(10))
    assert (count1 > 0, "Doctor not created")

    // Submit an existing doctor (should produce a different message).
    page = getPage ()
    navigateToUsaCfs (page)
    getButton (page, newDoctorButtonPath) .click()
    getForm ()
    city.setValueAttribute (city2)
    name.setValueAttribute (name2)
    surname.setValueAttribute (surname2)
    next.click()
    assert (getDiv (page, "//div[contains(text(), 'This practicioner is withing our database." +
      " Please go on with your rating!')]") ne null, page.asText)
    assert (getDiv (page, "//div[contains(text(), '" + name2 + "')]") ne null) // We see whose rating we edit.
    assert (getDiv (page, "//div[contains(text(), 'TYPE OF HEALTH PROFESSIONAL')]") ne null)

/*

2) - Look at the form -

  Check that there is a message describing what the page does and why.
  Check that there is a map of United States present.
See if there is also a textual selector of regions (e.g. states). Choose a state in the textual selector.
Look for the "next" button. It should not be present or should be disabled.
Look for the City field.
(In the future: Enter letter ... into the Citi field. Wait.
 Check that there is now a pop-up listing cities.
 Choose one of the cities from the pop-up.
 Find Doctor's name field. Click in it. Check that pop-up opened with the list of doctor names.)
Enter a random (unexisting) city name.
Check that the "next" button is still unactive.
Click the "next" button. Check that there is a pop-up asking us to enter Doctor's name.
Enter a random (unexisting) doctor name.
Click the "next" button. Check that there is a pop-up asking us to enter Doctor's surname.
Enter a random (unexisting) doctor surname.
Check that the "next" button is now enabled.
Check that there is a street address field. Fill it in with random data.
Click "next". Wait. See map and name-location form disappear,
replaced by a "thank you" message for entering a new doctor into the database and a prompt to continue with the rating.

(In the future, when HTMLUnit properly supports "back":
 Click "back", wait, see the form as we left it, with all data intact.
 Change street address, click "next", wait, see a "thank you for updating the data" message.
 Click "back", wait, click "forward", wait, see the rating without any special message.)

3) - Add the same doctor -
Open new page. Wait. *Login as another user*.
Click on United States, CFS. Wait.
Find the button which adds a doctor. Click it. Wait.
See the form. Enter the same data (region, city, doctor's name and surname) but leave street address blank.
Click "next". See the special message, that luckily we have found that doctor already existing in the database,
but ask the user to go on with their rating.

4) - Add the same doctor from the same user -
Open new page. Wait.
Click on United States, CFS. Wait. *Login as first user*.
Find the button which adds a doctor. Click it. Wait.
See the form. Enter the same data (region, city, doctor's name and surname) and maybe another random street address.
Click "next". See the special message, thanking the user for returning to the doctor's rating
and prompting him to continue with the rating, reviewing and updating the data as they see fit. 

 */

  } finally {
    for ((region, city, name, surname) <- TEMPORARY_DOCTORS) {
      val query = new Query ("Doctor")
      query.addFilter ("country", EQUAL, "usa")
      query.addFilter ("region", EQUAL, region)
      query.addFilter ("city", EQUAL, city)
      query.addFilter ("firstName", EQUAL, name)
      query.addFilter ("lastName", EQUAL, surname)
      for (doctor <- grond.model.util.queryToList (query)) {
        Datastore.SERVICE.delete (doctor.getKey)
      }
    }
  }
}
