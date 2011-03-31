package grond.client;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

/**
 * Information about the user which is currently signed in.
 */
public class GwtUser {
  public final String email, authDomain, userId, federatedIdentity;
  public final boolean isAdmin;

  public GwtUser(String json) {
    JSONObject user = JSONParser.parseStrict(json).isObject();
    email = getStringOrEmpty(user, "email");
    authDomain = getStringOrEmpty(user, "authDomain");
    userId = getStringOrEmpty(user, "userId");
    federatedIdentity = getStringOrEmpty(user, "federatedIdentity");
    isAdmin = user.get("isAdmin").isBoolean().booleanValue();
  }

  public static GwtUser fromJson(String user) {
    if (user == null || user.length() == 0) return null;
    return new GwtUser(user);
  }

  protected String getStringOrEmpty(JSONObject user, String field) {
    final JSONValue value = user.get(field);
    if (value == null) return "";
    final JSONString string = value.isString();
    if (string == null) return "";
    return string.stringValue();
  }
}
