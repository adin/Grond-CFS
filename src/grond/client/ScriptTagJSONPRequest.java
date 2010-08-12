package grond.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ResponseTextHandler;

/**
 * @author Janick Reynders
 */
interface JSONPResponseHandler {
  /**
   * Called when a JSONP request completes successfully.
   * 
   * @param responseText the JavaScript object (JSON) returned from the server
   */
  void onCompletion(JavaScriptObject responseObject);
}

/**
 * @author Janick Reynders
 */
public class ScriptTagJSONPRequest {
  private static Map callbacks = new HashMap();
  private static int currentCallbackNumber = 0;

  public static void asyncGet(String url, Map parameters, JSONPResponseHandler responseHandler) {
    final String callbackName = reserveCallback();
    registerCallBack(callbackName, responseHandler);
    addScript(callbackName, buildUrl(url, parameters, callbackName));
  }

  public static void asyncGet(String url, final String parameterName, final String parameterValue,
      JSONPResponseHandler responseHandler) {
    final HashMap parameters = new HashMap();
    parameters.put(parameterName, parameterValue);
    asyncGet(url, parameters, responseHandler);
  }

  public static void dispatchJSONPResponse(String callback, JavaScriptObject response,
      JSONPResponseHandler handler) {
    try {
      handler.onCompletion(response);
    } finally {
      cleanup(callback);
    }
  }

  public static void asyncRPCGet(String url, String rpcParameter, final ResponseTextHandler handler) {
    final JSONPResponseHandler responseHandler = new JSONPResponseHandler() {
      public void onCompletion(JavaScriptObject response) {
        JSONValue rpcResult = new JSONObject(response).get("rpcResult");
        handler.onCompletion(rpcResult.isString().stringValue());
      }
    };
    asyncGet(url, "rpc", rpcParameter, responseHandler);
  }

  private static native void addScript(String callbackName, String url) /*-{
    var elem = document.createElement("script");
    elem.setAttribute("charset", "UTF-8");
    elem.setAttribute("language", "JavaScript");
    elem.setAttribute("id", callbackName);
    elem.setAttribute("src", url);
    document.getElementsByTagName("head")[0].appendChild(elem);
  }-*/;

  private static String buildUrl(String url, Map parameters, String callback) {
    return url + "?callback=" + callback + toParameterList(parameters);
  }

  private static void cleanup(String callback) {
    unRegisterCallBack(callback);
    callbacks.values().remove(callback);
    removeScript(callback);
  }

  private static native void registerCallBack(String callback, JSONPResponseHandler handler) /*-{
    window[callback] = function(jsonObj) {
       @grond.client.ScriptTagJSONPRequest::dispatchJSONPResponse(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Lgrond/client/JSONPResponseHandler;)(callback, jsonObj, handler);
    }
  }-*/;

  private static native void removeScript(String callbackName) /*-{
    document.getElementsByTagName("head")[0].removeChild(document.getElementById(callbackName));
  }-*/;

  private static String reserveCallback() {
    while (true) {
      if (!callbacks.containsKey(new Integer(currentCallbackNumber))) {
        callbacks.put(new Integer(currentCallbackNumber), null);
        return "__gwt_callback" + currentCallbackNumber++;
      }
    }
  }

  private static String toParameterList(Map parameters) {
    String parameterList = "";
    Iterator iterator = parameters.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();
      parameterList += "&" + URL.encodeComponent((String) entry.getKey()) + "="
          + URL.encodeComponent((String) entry.getValue());
    }
    return parameterList;
  }

  private static native void unRegisterCallBack(String callback) /*-{
    window[callback] = null;
  }-*/;
}
