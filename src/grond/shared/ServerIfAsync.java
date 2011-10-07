package grond.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServerIfAsync {
  public void getDoctorsByRating(String country, String region, String condition, int limit,
      AsyncCallback<LinkedList<Doctor>> callback);
}
