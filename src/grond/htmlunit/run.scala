package grond.htmlunit

object run {
  def run = {
    import com.gargoylesoftware.htmlunit.{WebClient, BrowserVersion}
    import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlAnchor}
    val webClient = new WebClient (BrowserVersion.FIREFOX_3)
    try {

      // We have to really spin up some instances first for the HTMLUnit runs to have a chance!
      println ("grond.htmlunit.run: spinning up GAE instances (/grond/gae)!")
      val urlFetch = com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService
      val prefetch = new java.net.URL ("http://javagrond.appspot.com/grond/gae")
      for (i <- 0 until 5) {urlFetch.fetchAsync (prefetch)}

      // Might need a lot of time to fire a second instance of the GAE application.
      println ("webClient default timeout: " + webClient.getTimeout)
      webClient.setTimeout (60 * 1000)


    } finally {webClient.closeAllWindows}    
  }
}