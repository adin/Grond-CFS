package grond.client;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;

/** Success or error. */
class GaeResponse {
  public String errorMessage;
  public String successMessage;

  public String toString() {
    return errorMessage + successMessage;
  };
}

/** ScriptTag RPC to the cluster. */
public class Gae {
  /** {@link AsyncCallback} with standard error handler. */
  public abstract class ForwardingCallback<T, T2> implements AsyncCallback<T> {
    final AsyncCallback<T2> recipient;

    public ForwardingCallback(AsyncCallback<T2> recipient) {
      this.recipient = recipient;
    }

    public void onFailure(Throwable caught) {
      recipient.onFailure(caught);
    }
  }

  /** Invokes a servlet returning a string. */
  protected void gaeString(final AsyncCallback<String> callback, final String... parameters) {
    final JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
    jsonp.setTimeout(40000); // Default TCP/IP timeout + 10 seconds.

    String url = GWT.getModuleBaseURL() + "gae";
    for (int i = 0; i < parameters.length - 1; i += 2) {
      url += (i == 0 ? '?' : '&') + URL.encodeQueryString(parameters[i]) + '='
          + URL.encodeQueryString(parameters[i + 1]);
    }

    jsonp.requestString(url, callback);
  }

  /** Invokes a servlet returning either sucess or an error. */
  protected void gaeEither(final AsyncCallback<GaeResponse> callback, final String... parameters) {
    gaeString(new AsyncCallback<String>() {
      public void onFailure(final Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess(final String result) {
        final JSONValue json = JSONParser.parseStrict(result);
        final GaeResponse response = new GaeResponse();
        final JSONArray asArray = json.isArray();
        final JSONObject asObject = json.isObject();
        if (asArray != null) {
          JSONString jvs = asArray.get(0).isString();
          response.errorMessage = jvs != null ? jvs.stringValue() : "";
          jvs = asArray.get(1).isString();
          response.successMessage = jvs != null ? jvs.stringValue() : "";
        } else if (asObject != null) {
          response.errorMessage = asObject.get("errorMessage").isString().stringValue();
          response.successMessage = asObject.get("successMessage").isString().stringValue();
        } else throw new RuntimeException("Expected JSON object or array.");
        callback.onSuccess(response);
      }
    }, parameters);
  }

  protected void gaeUpdate(final String... parameters) {
    gaeString(new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
        Logger.getLogger("gaeUpdate").severe(caught.toString());
      }

      @Override
      public void onSuccess(String result) {
        if (result.length() != 0) Logger.getLogger("gaeUpdate").severe(result);
      }
    }, parameters);
  }

  /** See if the user is logged in with the server. */
  public void getCurrentUser(final AsyncCallback<GwtUser> callback) {
    gaeString(new ForwardingCallback<String, GwtUser>(callback) {
      public void onSuccess(String user) {
        //Logger.getLogger("Gae").info("Got user: " + user);
        recipient.onSuccess(GwtUser.fromJson(user));
      }
    }, "op", "getCurrentUser");
  }

  /** OpenID login. */
  public void createLoginURL(final String destinationURL, final String authDomain,
      final String federatedIdentity, final AsyncCallback<String> callback) {
    gaeString(new ForwardingCallback<String, String>(callback) {
      public void onSuccess(String url) {
        callback.onSuccess(url);
      }
    }, "op", "createLoginURL", "destinationURL", destinationURL, "authDomain", authDomain,
        "federatedIdentity", federatedIdentity);
  }

  protected void stringToObject(final String jsonString, final AsyncCallback<JSONObject> callback) {
    final JSONValue json = JSONParser.parseStrict(jsonString);
    final JSONString asString = json.isString();
    final JSONObject asObject = json.isObject();
    if (asString != null) callback.onFailure(new RuntimeException(asString.stringValue()));
    else if (asObject == null) callback.onFailure(new RuntimeException(
        "Returned JSON value is not an object."));
    else callback.onSuccess(asObject);
  }

  /** Creating a new doctor and rating.<br>
   * Returns rating values.
   * Also rating id in a 'ratingId' string and doctor values in a 'doctor' object.
   * Also 'doctorCreated' boolean is true if the doctor was created (false if existing doctor found).<br>
   * Returns an 'errorMessage' string if there was an error. */
  public void nameAndLocation(final String countryId, final String region, final String city,
      final String name, final String surname, final String problem, final AsyncCallback<JSONObject> callback) {
    gaeString(new ForwardingCallback<String, JSONObject>(callback) {
      public void onSuccess(String ratingOp) {
        stringToObject(ratingOp, callback);
      }
    }, "countryId", countryId, "region", region, "city", city, "name", name, "surname", surname, "problem",
        problem, "op", "nameAndLocation");
  }

  /** Rating values. Also rating id in a 'ratingId' string and doctor values in a 'doctor' object. */
  public void getRating(final String ratingId, final AsyncCallback<JSONObject> callback) {
    gaeString(new ForwardingCallback<String, JSONObject>(callback) {
      public void onSuccess(String rating) {
        stringToObject(rating, callback);
      }
    }, "ratingId", ratingId, "op", "getRating");
  }

  /** Update a list value (for example, a group of checkboxes) in the rating. */
  public void ratingUpdateList(final String ratingId, final String field, final String value,
      final boolean addOrRemove) {
    gaeUpdate("ratingId", ratingId, "field", field, "value", value, "vop", addOrRemove ? "add" : "remove",
        "op", "ratingUpdateList");
  }

  public void ratingUpdateString(final String ratingId, final String field, final String value) {
    gaeUpdate("ratingId", ratingId, "field", field, "value", value, "op", "ratingUpdateString");
  }

  public void ratingRemove(final String ratingId, final String field) {
    gaeUpdate("ratingId", ratingId, "field", field, "op", "ratingRemove");
  }
}
