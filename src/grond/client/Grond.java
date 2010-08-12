package grond.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.seventhdawn.gwt.rcx.client.RPCClientContext;

/**
 * Our main class.
 */
public class Grond implements EntryPoint {
  /** {@link AsyncCallback} with standard error handler. */
  public abstract class Callback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable caught) {
      serverError();
    }
  }

  private GaeAsync gae = null;

  /** The server side. */
  protected GaeAsync getGae() {
    if (gae == null) gae = GWT.create(Gae.class);
    return gae;
  }

  /** Displays a error message when AsyncCallback fails. */
  protected void serverError() {
    RootPanel page = RootPanel.get();
    page.add(new Label("An error occurred while "
        + "attempting to contact the server. Please check your network " + "connection and try again."));
  }

  public void onModuleLoad() {
    RootPanel.get().add(new Label("text to body"));

    final RootPanel page = RootPanel.get();
    conditionAndCountries(page);

    getGae().ping(new Callback<String>() {
      public void onSuccess(String result) {
        page.add(new Label(result));
      }
    });
  }

  /** Displays the condition and countries selector, leading to the map page of that country. */
  protected void conditionAndCountries(Panel panel) {

  }
}
