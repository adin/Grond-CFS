package grond.client;

import grond.shared.Countries.Country;
import grond.shared.Doctor;
import grond.shared.DoctorRating;
import grond.shared.SerializableWhiteList;
import grond.shared.ServerIf;
import grond.shared.ServerIfAsync;
import grond.shared.UserException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.jsonp.client.TimeoutException;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;

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
  protected final Grond grond;
  /** Maps request id to JSONP URL. Used to automatically repeat failed requests. */
  protected final HashMap<String, String[]> gaeRequests;
  protected final Storage localStorage = Storage.getLocalStorageIfSupported();
  protected JSONObject localCache;
  public boolean USE_GWT_RPC = false;
  final SerializationStreamFactory SSF = GWT.create(ServerIf.class);

  public Gae(final Grond grond, final HashMap<String, String[]> gaeRequests) {
    this.grond = grond;
    this.gaeRequests = gaeRequests;
  }

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

  protected JSONObject getLocalCache() {
    if (localCache != null) return localCache;
    if (localStorage == null) return null;
    try {
      final String grondGaeCache = localStorage.getItem("grondGaeCache");
      if (grondGaeCache != null) localCache = JSONParser.parseStrict(grondGaeCache).isObject();
      else localCache = new JSONObject();
      return localCache;
    } catch (Throwable ex) {
      Logger.getLogger("Gae.getLocalCache").severe(ex.toString());
      return null;
    }
  }

  protected void saveLocalCache(final String excludePattern) {
    final JSONObject localCache = getLocalCache();
    if (localCache == null || localStorage == null) return;

    final RegExp exclude = excludePattern == null ? null : RegExp.compile(excludePattern);

    // Remove expired entries.
    final JSONObject newCache = new JSONObject();
    try {
      final double time = (new java.util.Date()).getTime() / 1000;
      for (final String key : localCache.keySet()) {
        if (exclude != null && exclude.exec(key) != null) {
          Logger.getLogger("Gae.saveLocalCache").info(
              "excludePattern " + excludePattern + " matched for " + key);
          continue;
        }
        final JSONObject entry = localCache.get(key).isObject();
        final double expires = entry.get("expires").isNumber().doubleValue();
        if (time > expires) continue;
        newCache.put(key, entry);
      }
    } catch (Throwable ex) {
      Logger.getLogger("Gae.saveLocalCache").severe(ex.toString());
    }

    this.localCache = newCache;
    localStorage.setItem("grondGaeCache", newCache.toString());
  }

  protected void cache(final int ttlSec, final String key, final JSONValue value) {
    try {
      final JSONObject localCache = getLocalCache();
      if (localCache == null) return;
      final double time = (new java.util.Date()).getTime() / 1000;
      final JSONObject entry = new JSONObject();
      entry.put("value", value);
      entry.put("expires", new JSONNumber(time + ttlSec));
      localCache.put(key, entry);
      saveLocalCache(null);
    } catch (Throwable ex) {
      Logger.getLogger("Gae.cache").severe(ex.toString());
    }
  }

  /** Returns `null` if there is no entry. */
  protected JSONValue getCache(final String key, boolean includeExpired) {
    try {
      final JSONObject localCache = getLocalCache();
      if (localCache == null) return null;
      final JSONValue entryValue = localCache.get(key);
      if (entryValue == null) return null;
      final JSONObject entry = entryValue.isObject();
      if (entry == null) return null;
      final double expiresSec = entry.get("expires").isNumber().doubleValue();
      final double time = (new java.util.Date()).getTime() / 1000;
      final boolean isExpired = time > expiresSec;
      if (isExpired && !includeExpired) return null;
      return entry.get("value");
    } catch (Throwable ex) {
      Logger.getLogger("Gae.getCache").severe(ex.toString());
      return null;
    }
  }

  /** Remove entries matching `keyPattern`. */
  public void cleanCache(final String keyPattern) {
    saveLocalCache(keyPattern);
  }

  /** Invokes a servlet returning a string. */
  protected void gaeString(final AsyncCallback<String> callback, final String... parameters) {
    final JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
    // http://code.google.com/intl/en/appengine/docs/java/runtime.html#The_Request_Timer
    jsonp.setTimeout(40000); // Default TCP/IP timeout + 10 seconds.

    String url = GWT.getModuleBaseURL() + "gae";
    for (int i = 0; i < parameters.length - 1; i += 2) {
      if (parameters[i + 1] != null) url += (i == 0 ? '?' : '&') + URL.encodeQueryString(parameters[i]) + '='
          + URL.encodeQueryString(parameters[i + 1]);
    }

    jsonp.requestString(url, callback);
  }

  /** Hybrid RPC: uses script tag with JSON encoding to transfer the payload and then decode it using the GWT RPC. */
  protected <T> void gaeObject(final AsyncCallback<T> callback, final String... parameters) {
    gaeString(new ForwardingCallback<String, T>(callback) {
      @SuppressWarnings("unchecked") public void onSuccess(final String payload) {
        try {
          final Object obj = SSF.createStreamReader(payload).readObject();
          if (obj instanceof UserException) callback.onFailure((UserException) obj);
          else if (obj instanceof Exception) callback.onFailure((Exception) obj);
          else callback.onSuccess((T) obj);
        } catch (SerializationException sex) {
          callback.onFailure(sex);
        }
      }
    }, parameters);
  };

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

  /** @param rpcTargetId identifies what this request is changing (null if nothing). */
  protected void gaeUpdate(final String rpcTargetId, final String... parameters) {
    // Put the request into a queue in order not to try repeating requests which were superceeded by later requests.
    final String updateStamp = Arrays.asList(parameters).toString();
    if (rpcTargetId != null) gaeRequests.put(rpcTargetId, parameters);

    gaeString(new AsyncCallback<String>() {
      final AsyncCallback<String> callback = this;

      @Override public void onFailure(Throwable caught) {
        Logger.getLogger("gaeUpdate").severe(caught.toString());
        if (caught instanceof TimeoutException) {
          // See if this is a latest request for this target, and if it is, repeat it.
          if (rpcTargetId != null) {
            final String[] queuedParameters = gaeRequests.get(rpcTargetId);
            if (queuedParameters != null && Arrays.asList(queuedParameters).toString().equals(updateStamp)) {
              Logger.getLogger("gaeUpdate").info("Repeating request changing " + rpcTargetId + ".");
              gaeString(callback, parameters);
            }
          }
        }
      }

      @Override public void onSuccess(String result) {
        if (result.length() != 0) Logger.getLogger("gaeUpdate").severe(result);
        // Request successfull, remove it from the queue.
        if (rpcTargetId != null) {
          final String[] queuedParameters = gaeRequests.get(rpcTargetId);
          if (queuedParameters != null && Arrays.asList(queuedParameters).toString().equals(updateStamp)) gaeRequests
              .remove(rpcTargetId);
        }
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

  /** Creating a new doctor and rating.<br>
   * Returns rating values.
   * Also rating id in a 'ratingId' string and doctor values in a 'doctor' object.
   * Also 'doctorCreated' boolean is true if the doctor was created (false if existing doctor found).<br>
   * Returns an 'errorMessage' string if there was an error. */
  public void nameAndLocation(final String countryId, final String region, final String city,
      final String name, final String surname, final String problem,
      final AsyncCallback<DoctorRating> callback) {
    if (USE_GWT_RPC) {
      final ServerIfAsync server = GWT.create(ServerIf.class);
      server.nameAndLocation(countryId, region, city, name, surname, problem, callback);
      return;
    }
    gaeObject(callback, "countryId", countryId, "region", region, "city", city, "name", name, "surname",
        surname, "problem", problem, "op", "nameAndLocation");
  }

  /** Rating values. Also rating id in a 'ratingId' string and doctor values in a 'doctor' object. */
  public void getRating(final String ratingId, final AsyncCallback<DoctorRating> callback) {
    if (USE_GWT_RPC) {
      final ServerIfAsync server = GWT.create(ServerIf.class);
      server.getRating(ratingId, callback);
      return;
    }
    gaeObject(callback, "ratingId", ratingId, "op", "getRating");
  }

  /** Update a list value (for example, a group of checkboxes) in the rating. */
  public void ratingUpdateList(final String ratingId, final String field, final String value,
      final boolean addOrRemove) {
    gaeUpdate(ratingId + field, "ratingId", ratingId, "field", field, "value", value, "vop",
        addOrRemove ? "add" : "remove", "op", "ratingUpdateList");
  }

  public void ratingUpdateString(final String ratingId, final String field, final String value) {
    gaeUpdate(ratingId + field, "ratingId", ratingId, "field", field, "value", value, "op",
        "ratingUpdateString");
  }

  public void ratingRemove(final String ratingId, final String field) {
    gaeUpdate(ratingId + field, "ratingId", ratingId, "field", field, "op", "ratingRemove");
  }

  /**
   * The list of country doctors sorted by condition-related rating (cfsRating, fmRating).<br>
   * Note: the last element returned will be a "There's more." string if the database had more than `limit` doctors.
   */
  public void getDoctorsByRating(final Country country, final String $region, final String condition,
      final int limit, final AsyncCallback<LinkedList<Doctor>> callback) {
    if (USE_GWT_RPC) {
      final ServerIfAsync server = GWT.create(ServerIf.class);
      server.getDoctorsByRating(country.id, $region, condition, limit, callback);
      return;
    }

    final String region = $region == null ? "" : $region;
    // TODO Restore caching?
    //    final String cacheKey = "getDoctorsByRating, " + country + ", " + region + ", " + condition + ", "
    //        + limit;
    //    final JSONValue have = getCache(cacheKey, true);
    //    if (have != null) {
    //      callback.onSuccess(have.isArray());
    //    }

    gaeObject(callback, "op", "getDoctorsByRating", "country", country.id, "region", region, "condition",
        condition, "limit", Integer.toString(limit));
  }

  public void getDoctorTRP(final long doctorId, final boolean needDoctorInfo,
      final AsyncCallback<Doctor> callback) {
    if (USE_GWT_RPC) {
      final ServerIfAsync server = GWT.create(ServerIf.class);
      server.getDoctorTRP(doctorId, needDoctorInfo, callback);
    } else gaeObject(callback, "op", "getDoctorTRP", "doctorId", Long.toString(doctorId), "needDoctorInfo",
        needDoctorInfo ? "true" : "false");
  }

  public void getDoctorSuggestions(final String region, final String city,
      final AsyncCallback<JSONObject> callback) {
    gaeString(new ForwardingCallback<String, JSONObject>(callback) {
      public void onSuccess(final String array) {
        recipient.onSuccess(JSONParser.parseStrict(array).isObject());
      }
    }, "op", "getDoctorSuggestions", "region", region, "city", city);
  }

  public void serializableWhiteList(final AsyncCallback<SerializableWhiteList> callback) {
    final ServerIfAsync server = GWT.create(ServerIf.class);
    server.serializableWhiteList(new SerializableWhiteList(), callback);
  }
}
