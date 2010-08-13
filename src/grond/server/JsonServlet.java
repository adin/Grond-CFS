package grond.server;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.repackaged.org.json.JSONObject;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class JsonServlet extends RemoteServiceServlet {
  // http://timepedia.blogspot.com/2009/04/gwt-rpc-over-arbitrary-transports-uber.html

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
      IOException {
    try {
      synchronized (this) {
        if (perThreadRequest == null) perThreadRequest = new ThreadLocal<HttpServletRequest>();
        if (perThreadResponse == null) perThreadResponse = new ThreadLocal<HttpServletResponse>();
        perThreadRequest.set(request);
        perThreadResponse.set(response);
      }

      // Read the request fully.
      //
      String requestPayload = readJsonContent(request);

      // Let subclasses see the serialized request.
      //
      onBeforeRequestDeserialized(requestPayload);

      // Invoke the core dispatching logic, which returns the serialized
      // result.
      //
      String responsePayload = processCall(requestPayload);

      // Let subclasses see the serialized response.
      //
      onAfterResponseSerialized(responsePayload);

      // Write the response.
      //
      writeJsonResponse(request, response, responsePayload);
    } catch (Exception e) {
      doUnexpectedFailure(e);
    } finally {
      perThreadRequest.set(null);
      perThreadResponse.set(null);
    }
  }

  protected String readJsonContent(HttpServletRequest httpServletRequest) throws ServletException, IOException {
    String str = httpServletRequest.getMethod().equals("POST") ? RPCServletUtils.readContentAsUtf8(
        httpServletRequest, false) : httpServletRequest.getParameter("rpc");
    String ustr = URLDecoder.decode(str, "UTF-8");
    return ustr;
  }

  protected void writeJsonResponse(HttpServletRequest request, HttpServletResponse response, String payload)
      throws IOException {
    String callback = request.getParameter("callback");
    JSONObject object = new JSONObject();
    try {
      response.setContentType("text/javascript");
      object.put("rpcResult", payload);
      response.getWriter().write(callback + "(" + object + ")");
    } catch (Exception ex) {
      getServletContext().log("Unable to compress response", ex);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
