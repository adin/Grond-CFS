package grond.client;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
public interface OldGae {
  public static class GwtUser implements Serializable {
    private static final long serialVersionUID = 1L;
    public String email, authDomain, userId, federatedIdentity;
    public boolean isAdmin;
  }

  /** See if the user is logged in with the server. */
  GwtUser getCurrentUser();

  /** OpenID login. */
  String createLoginURL(String destinationURL, String authDomain, String federatedIdentity);

  /** OpenId logout. */
  String createLogoutURL(String destinationURL);

  /** Unit tests related to the server side of the application. */
  String internalTests();
}
