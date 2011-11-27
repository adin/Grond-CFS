package grond.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Extend the list of serializable classes.<br>
 * https://groups.google.com/group/google-web-toolkit/browse_thread/thread/9eb513e449be3940/
 */
public class SerializableWhiteList implements IsSerializable {
  @SuppressWarnings("unused") private Double aDouble;
  @SuppressWarnings("unused") private int[] intArray;
}
