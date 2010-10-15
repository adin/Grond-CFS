package grond.client;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.seventhdawn.gwt.rcx.client.annotation.ClientProxySuperclass;
import com.seventhdawn.gwt.rpc.scripttag.client.ScriptTagServiceProxy;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("gae")
@ClientProxySuperclass(ScriptTagServiceProxy.class)
public interface Gae extends RemoteService {
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
  public String createLogoutURL(String destinationURL);

  /** Unit tests related to the server side of the application. */
  String internalTests();
}
