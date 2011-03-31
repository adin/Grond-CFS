package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient, BrowserVersion}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlElement,
  HtmlTable, HtmlButton, HtmlDivision, HtmlInput, HtmlAnchor}

/** Some static variables and methods. */
object fun {
  /** Cached WebClient. */
  val FIREFOX3: WebClient = {
    // Tone down the weird HTMLUnit warnings.
    try {
      org.apache.commons.logging.LogFactory.getLog ("com.gargoylesoftware.htmlunit") match {
        case logger: org.apache.commons.logging.impl.Jdk14Logger =>
          logger.getLogger.setLevel (java.util.logging.Level.SEVERE)
        case ul => println ("grond.htmlunit.run: Unknown logger: " + ul.getClass.getName)
      }
    } catch {case ex => ex.printStackTrace}

    new WebClient (BrowserVersion.FIREFOX_3)
  }

  /** Force AppEngine to spawn extra application instances. */
  def gaeSpinUp: Unit = {
    // We have to really spin up some instances first for the HTMLUnit runs to have a chance!
    val urlFetch = com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService
    val prefetch = new java.net.URL ("http://javagrond.appspot.com/grond/gae?spinUp")
    for (i <- 0 until 5) {urlFetch.fetchAsync (prefetch)}
  }

  def randomAlphabeticalString (random: java.util.Random) = new String (new Array[Byte](0) ++ (
    for (num <- 2 until 9) yield ('A' + random.nextInt('Z' - 'A')).toByte
    ), "US-ASCII")
}

/** Abstract HTMLUnit test. */
abstract class Test (val webClient: WebClient, val hostUrl: String) {
  def getPage () = webClient.getPage (hostUrl) .asInstanceOf[HtmlPage]
  def getPage (url: String) = webClient.getPage (url) .asInstanceOf[HtmlPage]

  /** HtmlUnit fails on login and logout redirects. This function will finish the job. */
  def redirectingClick (clickOn: HtmlElement, fallbackToURL: String = null): HtmlPage = {
    webClient.getCache.clear // Make sure we actually invoke the server.
    import com.gargoylesoftware.htmlunit.{FailingHttpStatusCodeException => NotFount}
    try {
      clickOn.click()
    } catch {case knownProblem: Throwable if knownProblem.getMessage contains "/_ah/grond/grond.nocache.js" =>
      // There is a problem with HtmlUnit not handling the final redirect properly, but the login still works:
      if (fallbackToURL eq null) getPage() else webClient.getPage (fallbackToURL)
    }
  }

  def signOut (havePage: Option[HtmlPage]): HtmlPage = {
    webClient.removeRequestHeader ("MockUserEmail")
    var page: HtmlPage = if (havePage.isDefined) havePage.get else getPage()
    if (getDiv (page, "//div[starts-with(text(),'Hi, you are signed in as ')]") ne null) {
      page = redirectingClick (getAnchor (page, "//a[starts-with(text(),'Sign out')]"))
      assert (getDiv (page, "//div[text()='You are not currently signed in!']") ne null)
    }
    page
  }

  /** If the current page is a login form (either Development Mode one or the Google Accounts one)
   * then this method will sign in. For Development Mode it will use the login form
   * and for Google Accounts mode it will use the mock user object. */
  def signIn2 (_page: HtmlPage, testEmail: String, continueToHistory: String): HtmlPage = {
    // - Login under a given test user -
    // If the Development Mode login form is present, we just enter the desired user email there,
    // otherwise we use a "mock user object" scheme to workaround the login.
    var page = _page
    val loginPage = page.asXml
    // Development Mode login: http://paste.pocoo.org/show/291596/
    if ((loginPage contains "isAdmin") && (loginPage contains "Sign in as Administrator")) {
      // We seem to be in the Development Mode login page.
      val continueInput = getInput (page, "//input[@name='continue']")
      val continue = continueInput.getValueAttribute
      assert (continue == hostUrl + continueToHistory, continue)
      val emailInput = getInput (page, "//input[@name='email']")
      assert (emailInput ne null, loginPage)
      emailInput.setValueAttribute (testEmail)
      val logIn = getInput (page, "//input[@type='submit' and @name='action' and @value='Log In']")
      page = redirectingClick (logIn, continue)
      assert (getDiv (page, "//div[text()='Hi, you are signed in as " + testEmail + "!']") ne null)
    // Google Login: http://paste.pocoo.org/show/nFtu9k9ptLMH123MKHwJ/
    } else if (loginPage contains "is asking for some information from your") {
      assert (hostUrl startsWith "http://javagrond.appspot.com/", hostUrl) // Google login only happens there.
      // Request Mock Object for the given user.
      webClient.addRequestHeader("MockUserEmail", testEmail)
      page = webClient.getPage (hostUrl + continueToHistory)
      assert (page.asText contains "Hi, you are signed in as " + testEmail + "!", page.asText)
    } else {
      println (page.asXml)
      throw new Exception ("Not a login page? Login redirection failed?")
    }
    page
  }

  /** From the main page to the #mapOf_usa_cfs page. */
  def navigateToUsaCfs (page: HtmlPage): Unit = {
    val countryTable = getTable (page, "//table[@id='countryTable']")
    assert (countryTable.getCellAt (0, 0) .asText == "Chronic Fatigue Syndrome")
    assert (countryTable.getCellAt (1, 0) .asText == "United States")
    countryTable.getCellAt (1, 0) .getFirstByXPath ("a") .asInstanceOf[HtmlAnchor] .click()
  }

  /** See <a href="http://www.w3schools.com/XPath/xpath_syntax.asp">XPath syntax</a>.<br>
   * Cheat-sheet: "<code>//table[@id='___']</code>". */
  def getTable (page: HtmlPage, xpath: String) = page.getFirstByXPath (xpath) .asInstanceOf[HtmlTable]
  def getButton (page: HtmlPage, xpath: String) = page.getFirstByXPath (xpath) .asInstanceOf[HtmlButton]
  def getDiv (page: HtmlPage, xpath: String) = page.getFirstByXPath (xpath) .asInstanceOf[HtmlDivision]
  def getInput (page: HtmlPage, xpath: String) = page.getFirstByXPath (xpath) .asInstanceOf[HtmlInput]
  def getAnchor (page: HtmlPage, xpath: String) = page.getFirstByXPath (xpath) .asInstanceOf[HtmlAnchor]

  /** Overriden by the test. */
  def run: Unit
}
