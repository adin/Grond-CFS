package grond.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServerIfAsync {
  public void getDoctorsByRating(String country, String region, String condition, int limit,
      AsyncCallback<LinkedList<Doctor>> callback);

  void nameAndLocation(String countryId, String region, String city, String name, String surname,
      String problem, AsyncCallback<DoctorRating> callback);

  void getRating(String ratingId, AsyncCallback<DoctorRating> callback);
}
