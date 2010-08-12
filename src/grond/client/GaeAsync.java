package grond.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link Gae}.
 */
public interface GaeAsync {
  void ping(AsyncCallback<String> callback);
}
