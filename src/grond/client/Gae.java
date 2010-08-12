package grond.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.seventhdawn.gwt.rcx.client.RPCContextServiceProxy;
import com.seventhdawn.gwt.rcx.client.annotation.ClientProxySuperclass;
import com.seventhdawn.gwt.rcx.client.annotation.CustomSerializableRoots;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("gae")
@ClientProxySuperclass(MyServiceProxy.class)
public interface Gae extends RemoteService {
  String ping();
}
