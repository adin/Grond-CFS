package grond.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServerIfAsync {
  void serializableWhiteList(SerializableWhiteList swl, AsyncCallback<SerializableWhiteList> callback);

  public void getDoctorsByRating(String country, String region, String condition, int limit,
      AsyncCallback<LinkedList<Doctor>> callback);

  void nameAndLocation(String countryId, String region, String city, String name, String surname,
      String problem, AsyncCallback<DoctorRating> callback);

  void getRating(String ratingId, AsyncCallback<DoctorRating> callback);

  void getDoctorTRP(long doctorId, boolean needDoctorInfo, AsyncCallback<Doctor> callback);
}
