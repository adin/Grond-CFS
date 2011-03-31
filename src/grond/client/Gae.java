package grond.client;

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

  /** Creating a new doctor and rating. Returns ratingId. */
  public void nameAndLocation(final String countryId, final String region, final String city,
      final String name, final String surname, final String problem, final AsyncCallback<GaeResponse> callback) {
    gaeEither(new ForwardingCallback<GaeResponse, GaeResponse>(callback) {
      public void onSuccess(GaeResponse rep) {
        callback.onSuccess(rep);
      }
    }, "countryId", countryId, "region", region, "city", city, "name", name, "surname", surname, "problem",
        problem, "op", "nameAndLocation");
  }
}
