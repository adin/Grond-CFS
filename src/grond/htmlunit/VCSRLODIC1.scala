package grond.htmlunit

import com.gargoylesoftware.htmlunit.{WebClient}
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlAnchor}

class VCSRLODIC1 (webClient: WebClient, hostUrl: String) extends Test (webClient, hostUrl) {
  override def run: Unit = {
      val log = org.apache.commons.logging.LogFactory.getLog ("com.gargoylesoftware.htmlunit")
      log match {
        case logger: org.apache.commons.logging.impl.Jdk14Logger =>
          logger.getLogger.setLevel (java.util.logging.Level.SEVERE)
        case ul => println ("grond.htmlunit.run: Unknown logger: " + ul.getClass.getName)
      }

      println (<span id="SVCCC">SVCCC: As site visitor, I can choose a condition (CFS/FM) and a country in which I am interested, to evaluate or improve the available healthcare information within that country.</span>)
  
      val page = webClient.getPage ("http://javagrond.appspot.com/") .asInstanceOf[HtmlPage]
      assert (page.getTitleText == "GROND")
  
      // Navigate to United States, FM.
      val countryTable = page.getHtmlElementById ("countryTable") .asInstanceOf[HtmlTable]
      assert (countryTable.getCellAt (0, 1) .asText == "Fibromyalgia")
      assert (countryTable.getCellAt (1, 1) .asText == "United States")
      countryTable.getCellAt (1, 1) .getFirstByXPath ("a") .asInstanceOf[HtmlAnchor] .click
  
      assert (page.asText contains "Rating for United States")
  
      println ("""<script type="text/javascript">
        document.getElementById ("SVCCC") .style.color = "green"
      </script>""")
  
      //cms.writebr (<textarea style="width: 99%">{page.asXml}</textarea>)
  }
}
