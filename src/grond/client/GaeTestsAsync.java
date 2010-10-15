package grond.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link GaeTests}.
 */
public interface GaeTestsAsync {
  void gaeSpinUp(AsyncCallback<Void> callback);
  void SVCCC(AsyncCallback<String> callback);
  void VCSRLODIC1(AsyncCallback<String> callback);
}
