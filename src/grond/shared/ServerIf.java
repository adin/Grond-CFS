package grond.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/** This GWT RPC interface currently isn't used directly,
 * but it is here to provide proper method signatures for `RPC.encodeResponseForSuccess`. */
@RemoteServiceRelativePath("nativeGae") public interface ServerIf extends RemoteService {
  public SerializableWhiteList serializableWhiteList (SerializableWhiteList swl);

  public LinkedList<Doctor> getDoctorsByRating(String country, String region, String condition, int limit)
      throws UserException;

  public DoctorRating nameAndLocation(String countryId, String region, String city, String name,
      String surname, String problem) throws UserException;

  public DoctorRating getRating(String ratingId) throws UserException;

  public Doctor getDoctorTRP(long doctorId, boolean needDoctorInfo) throws UserException;
}
