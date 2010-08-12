package grond.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.Serializer;

/**
 * Cross-site RPC using &lt;script&gt;.<br>
 * Based on GWT 1.4 patch by Janick Reynders.         
 */
public class MyServiceProxy extends RemoteServiceProxy {
  private static final String RPC_CONTENT_TYPE = "text/x-gwt-rpc; charset=utf-8";
  private RpcRequestBuilder rpcRequestBuilder;

  protected MyServiceProxy(String moduleBaseURL, String remoteServiceRelativePath,
      String serializationPolicyName, Serializer serializer) {
    super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);
  }

  static boolean isReturnValue(String encodedResponse) {
    return encodedResponse.startsWith("//OK");
  }

  static boolean isThrownException(String encodedResponse) {
    return encodedResponse.startsWith("//EX");
  }

  @Override
  protected <T> Request doInvoke(final ResponseReader responseReader, String methodName, int invocationCount,
      String requestData, final AsyncCallback<T> callback) {
    try {
      ScriptTagJSONPRequest.asyncGet(getServiceEntryPoint(), "rpc", requestData, new JSONPResponseHandler() {
        public void onCompletion(JavaScriptObject response) {
          JSONObject jsonObject = new JSONObject(response);
          JSONValue rpcResult = jsonObject.get("rpcResult");
          String encodedResponse = rpcResult.isString().stringValue();
          try {
            if (encodedResponse == null) {
              callback.onFailure(new InvocationException("No response payload"));
            } else if (isReturnValue(encodedResponse)) {
              callback.onSuccess((T) responseReader.read(createStreamReader(encodedResponse)));
            } else if (isThrownException(encodedResponse)) {
              callback.onFailure((Throwable) createStreamReader(encodedResponse).readObject());
            }
          } catch (SerializationException e) {
            callback.onFailure(e);
          }
        }
      });
    } catch (Exception ex) {
      InvocationException iex = new InvocationException(
          "Unable to initiate the asynchronous service invocation -- check the network connection", ex);
      callback.onFailure(iex);
    } finally {
      if (RemoteServiceProxy.isStatsAvailable()
          && RemoteServiceProxy.stats(RemoteServiceProxy.bytesStat(methodName, invocationCount,
              requestData.length(), "requestSent"))) {}
    }
    return null;
  }
}
