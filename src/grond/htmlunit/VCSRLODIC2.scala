package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlAnchor, HtmlButton}
import scala.collection.JavaConversions._

/**
 * Going to main page and clicking on CFS/United States produces a button allowing us to rate a new doctor.
 */
class VCSRLODIC2 (webClient: WebClient, hostUrl: String) extends Test (webClient, hostUrl) {
  override def run: Unit = {
    val page = getHtmlPage ()
    webClient.waitForBackgroundJavaScript (1000)
  
    // Navigate to United States, CFS.
    val countryTable = getHtmlTable (page, "//table[@id='countryTable']")
    assert (countryTable.getCellAt (0, 0) .asText == "Chronic Fatigue Syndrome")
    assert (countryTable.getCellAt (1, 0) .asText == "United States")
    countryTable.getCellAt (1, 0) .getFirstByXPath ("a") .asInstanceOf[HtmlAnchor] .click
    webClient.waitForBackgroundJavaScript (1000)

    // See if there is a button to rate new doctors.
    var button: HtmlButton = null
    page.getByXPath("//button[@class='gwt-Button']").foreach {case but: HtmlButton =>
      if ((but.asText contains "doctor") && (but.asText contains "Click here to add")) button = but
    }
    assert (button ne null)
  }
}
