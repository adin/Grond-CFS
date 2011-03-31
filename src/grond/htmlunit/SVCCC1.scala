package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient, TopLevelWindow, History}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlAnchor}

/** Going to "http://javagrond.appspot.com/" and clicking on "United States"
 * produces a page with information about United States. */
class SVCCC (webClient: WebClient, hostUrl: String) extends Test (webClient, hostUrl) {
  override def run: Unit = {
    val page = getPage ()
    assert (page.getTitleText == "GROND")

    // Navigate to United States, FM.
    val countryTable = getTable (page, "//table[@id='countryTable']")
    assert (countryTable.getCellAt (0, 1) .asText == "Fibromyalgia")
    assert (countryTable.getCellAt (1, 1) .asText == "United States")
    countryTable.getCellAt (1, 1) .getFirstByXPath ("a") .asInstanceOf[HtmlAnchor] .click

    var text = page.asText
    assert (text contains "Rating for United States")
    assert (text contains "Map for United States")

    // SVCCC2: Clicking the "back" browser button returns us to the list of countries.

    // SVCCC2 is impossible as of HTMLUnit 2.8 - history isn't changed and isn't reacted to.
    // TODO: Try testing history under new versions of HTMLUnit when they're out.
    if (true) return

    // delme: Checking if history works in HTMLUnit.
    import scala.collection.JavaConversions._
    for (window <- webClient.getWebWindows) {
      println ("url_ " + window.getHistory.getUrl (window.getHistory.getIndex))
    }

    val window = webClient.getWebWindows.get (0) .asInstanceOf[TopLevelWindow]
    val history = window.getHistory
    def url = history.getUrl (history.getIndex) .toString
    assert (url endsWith "#mapOf_usa_fm", url)

    history.back
    assert (!(url endsWith "#mapOf_usa_fm"), url)

    // Check that we have returned to the list of countries.
    text = page.asText
    assert (text contains "Fibromyalgia")
    assert (text contains "Australia")
  }
}
