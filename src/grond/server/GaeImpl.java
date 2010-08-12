package grond.server;

import grond.client.Gae;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GaeImpl extends JsonServlet implements Gae {
  public String ping() {
    System.out.println ("GaeImpl: ping received!");
    return getServletContext().getServerInfo();
  }
}
