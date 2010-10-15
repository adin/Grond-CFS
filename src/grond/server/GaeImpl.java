package grond.server;

import grond.client.Gae;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GaeImpl extends RemoteServiceServlet implements Gae {
  public Gae.GwtUser getCurrentUser() {
    User user = UserServiceFactory.getUserService().getCurrentUser();
    if (user == null) return null;
    Gae.GwtUser gwtUser = new Gae.GwtUser();
    gwtUser.email = user.getEmail();
    gwtUser.authDomain = user.getAuthDomain();
    gwtUser.userId = user.getUserId();
    gwtUser.federatedIdentity = user.getFederatedIdentity();
    gwtUser.isAdmin = UserServiceFactory.getUserService().isUserAdmin();
    return gwtUser;
  }

  //  public String createLoginURL(String destinationURL) {
  //    return UserServiceFactory.getUserService().createLoginURL(destinationURL);
  //  }

  public String createLoginURL(String destinationURL, String authDomain, String federatedIdentity) {
    return UserServiceFactory.getUserService().createLoginURL(destinationURL, authDomain, federatedIdentity,
        new HashSet<String>());
  }

  public String createLogoutURL(String destinationURL) {
    return UserServiceFactory.getUserService().createLogoutURL(destinationURL);
  }

  protected void pass(StringBuilder sb) {
    sb.append("<span style=\"color: green\">pass</span>.<br/>");
  }

  protected void fail(StringBuilder sb, Throwable ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    sb.append("<span style=\"color: red\">fail</span>!<pre style=\"color: red; font-size: smaller\">")
        .append(sw.toString()).append("</pre>");
  }

  public String internalTests() {
    StringBuilder sb = new StringBuilder();

    sb.append("doctorNameAndLocation .. ");
    try {
      grond.model.doctorNameAndLocation._test();
      pass(sb);
    } catch (Throwable ex) {
      fail(sb, ex);
    }

    return sb.toString();
  }
}
