package grond.server
import javax.servlet.http._

/**
 * Servlet interfacing to unit and acceptance tests.
 */
class GaeTestsImpl extends HttpServlet {
  protected def getHostUrl (request: HttpServletRequest): String = {
    try {
      val port = request.getLocalPort
      var localName = request.getLocalName
      if (localName == null) localName = "javagrond.appspot.com" // Happens to be null when deployed.
      if (localName.equals ("0:0:0:0:0:0:0:1")) localName = "localhost" // Happens on test server.
      return request.getScheme + "://" + localName + ":" + (if (port == 0 || port == 80) "" else port) + "/"
    } catch {case ex =>
      ex.printStackTrace();
      return "http://127.0.0.1:8888/";
    }
  }

  def trace (ex: Throwable): String = {
    val sw = new java.io.StringWriter
    val pw = new java.io.PrintWriter (sw)
    ex.printStackTrace (pw)
    pw.close
    sw.toString
  }

  def internalTests: String = {
    val sb = new StringBuilder();

    def pass = sb.append ("<span style=\"color: green\">pass</span>.<br/>");
    def fail (ex: Throwable) =
      sb.append ("<span style=\"color: red\">fail</span>!<pre style=\"color: red; font-size: smaller\">")
        .append (trace (ex)) .append ("</pre>");

    sb.append ("doctorNameAndLocation .. ")
    try {grond.model.doctorNameAndLocation._test; pass} catch {case ex => fail (ex)}
    sb.toString
  }

  /** Servlet's main method.<br>
   * Static files are handled separately by the web server,
   * that leaves us with handling of Velocity templates and form actions. */
  protected def welcome (request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType ("text/javascript")
    response.setCharacterEncoding ("utf-8")
    
    val callback = request.getParameter ("callback");
    if (callback eq null) return;

    def respond (str: String): Unit = {
      val writer = response.getWriter
      writer.write (callback)

      // http://code.google.com/p/json-simple/
      //writer.write (" (\"")
      //writer.write (org.json.simple.JSONObject.escape (str))
      //writer.write ("\");")

      // GAE http://www.json.org/javadoc/org/json/JSONObject.html
      writer.write (" (")
      writer.write (com.google.appengine.repackaged.org.json.JSONObject.quote (str))
      writer.write (");")
    }
    
    if (request.getParameter ("gaeSpinUp") == "true") {respond ("okay"); return}
    if (request.getParameter ("internalTests") == "true") {respond (internalTests); return}

    try {
      val hostUrl = getHostUrl (request)
      val firefox = grond.htmlunit.fun.FIREFOX3
      firefox.synchronized { // On development server under Jetty this can run in parallel and face conflicts.
        request.getParameter ("test") match {
          case "SVCCC" => new grond.htmlunit.SVCCC (firefox, hostUrl) .run
          case "VCSRLODIC1" => new grond.htmlunit.VCSRLODIC1 (firefox, hostUrl) .run
          case "VCSRLODIC2" => new grond.htmlunit.VCSRLODIC2 (firefox, hostUrl) .run
        }
      }
      respond ("okay")
    } catch {case ex => respond (trace (ex))}
  }

  override def doGet (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
  override def doPost (request: HttpServletRequest, response: HttpServletResponse): Unit = welcome (request, response)
}
