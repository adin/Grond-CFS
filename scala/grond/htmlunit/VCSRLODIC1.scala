package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlAnchor}

/**
 * Going to main page and clicking on CFS/United States produces a non-empty table of doctors and their CFS ratings.
 */
class VCSRLODIC1 (webClient: WebClient, hostUrl: String) extends Test (webClient, hostUrl) {
  override def run: Unit = {
    val page = getPage ()
    navigateToUsaCfs (page)

    // See if there is a non-empty table of doctors there.
    val topDoctors = getTable (page, "//table[@id='topDoctors']")
    assert (topDoctors ne null)
    assert (topDoctors.getCellAt (0, 0) .asText == "Name")
    assert (topDoctors.getCellAt (0, 1) .asText == "State")
    assert (topDoctors.getCellAt (0, 2) .asText == "City")
    assert (topDoctors.getCellAt (0, 3) .asText == "Rating")
    assert (topDoctors.getRowCount >= 2) // Header and at least one doctor.
    assert (topDoctors.getCellAt (1, 4) .asText == "Rate!") // Button to rate the doctor.
  }
}
