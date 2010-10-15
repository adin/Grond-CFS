package grond.server;

import static grond.htmlunit.fun.FIREFOX3;
import grond.client.GaeTests;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GaeTestsImpl_delme extends RemoteServiceServlet implements GaeTests {
  protected String hostUrl() {
    try {
      final HttpServletRequest request = perThreadRequest.get();
      final int port = request.getLocalPort();
      String localName = request.getLocalName();
      if (localName == null) localName = "javagrond.appspot.com"; // Happens to be null when deployed.
      if (localName.equals ("0:0:0:0:0:0:0:1")) localName = "localhost"; // Happens on test server.
      return request.getScheme() + "://" + localName + ":" + (port == 0 || port == 80 ? "" : port) + "/";
    } catch (Exception ex) {
      ex.printStackTrace();
      return "http://127.0.0.1:8888/";
    }
  }

  protected String trace(Throwable ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  public void gaeSpinUp() {
    grond.htmlunit.fun.gaeSpinUp();
  }

  public String SVCCC() {
    try {
      new grond.htmlunit.SVCCC(FIREFOX3(), hostUrl()).run();
    } catch (Throwable ex) {
      return trace(ex);
    }
    return "okay";
  }

  public String VCSRLODIC1() {
    try {
      new grond.htmlunit.VCSRLODIC1(FIREFOX3(), hostUrl()).run();
    } catch (Throwable ex) {
      return trace(ex);
    }
    return "okay";
  }
}
