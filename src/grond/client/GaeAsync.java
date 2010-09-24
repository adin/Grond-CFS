package grond.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link Gae}.
 */
public interface GaeAsync {
  void ping(AsyncCallback<String> callback);

  void getCurrentUser(AsyncCallback<Gae.GwtUser> callback);

  void createLoginURL(String destinationURL, String authDomain, String federatedIdentity,
      AsyncCallback<String> callback);

  void createLogoutURL(String destinationURL, AsyncCallback<String> callback);

  void internalTests(AsyncCallback<String> callback);
}
