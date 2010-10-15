package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient, BrowserVersion}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable}

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
}

/** Abstract HTMLUnit test. */
abstract class Test (val webClient: WebClient, val hostUrl: String) {
  def getHtmlPage () = webClient.getPage (hostUrl) .asInstanceOf[HtmlPage]
  def getHtmlPage (url: String) = webClient.getPage (url) .asInstanceOf[HtmlPage]
  /** See <a href="http://www.w3schools.com/XPath/xpath_syntax.asp">XPath syntax</a>.<br>
   * Cheat-sheet: "<code>//table[@id='___']</code>". */
  def getHtmlTable (page: HtmlPage, xpath: String) = page.getFirstByXPath(xpath) .asInstanceOf[HtmlTable]

  /** Overriden by the test. */
  def run: Unit
}
