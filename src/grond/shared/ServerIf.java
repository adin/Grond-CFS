package grond.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.RemoteService;

/** This GWT RPC interface currently isn't used directly,
 * but it is here to provide proper method signatures for `RPC.encodeResponseForSuccess`. */
public interface ServerIf extends RemoteService {
  public LinkedList<Doctor> getDoctorsByRating(String country, String region, String condition, int limit);
}
