package grond.client;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.seventhdawn.gwt.rcx.client.annotation.ClientProxySuperclass;
import com.seventhdawn.gwt.rpc.scripttag.client.ScriptTagServiceProxy;

/**
 * Acceptance tests (with HTMLUnit).<br>
 * A test would return an "okay" message if it pass,
 * error stack trace if it fails.
 */
@RemoteServiceRelativePath("tests")
@ClientProxySuperclass(ScriptTagServiceProxy.class)
public interface GaeTests extends RemoteService {
  void gaeSpinUp();

  String SVCCC();

  String VCSRLODIC1();
}
