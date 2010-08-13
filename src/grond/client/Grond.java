package grond.client;

import grond.shared.Countries;
import grond.shared.Countries.Country;

import java.util.Arrays;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.maddison.gwt.logging.client.Logger;
import com.maddison.gwt.logging.client.Logging;

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

  private Logger log = null;
  private GaeAsync gae = null;

  /** The server side. */
  protected GaeAsync getGae() {
    if (gae == null) gae = GWT.create(Gae.class);
    return gae;
  }

  protected Logger getLog() {
    if (log == null) log = Logging.getLogger();
    return log;
  }

  /** Displays a error message when AsyncCallback fails. */
  protected void serverError() {
    RootPanel page = RootPanel.get();
    page.add(new Label("An error occurred while "
        + "attempting to contact the server. Please check your network " + "connection and try again."));
  }

  public void onModuleLoad() {
    final Document document = Document.get();
    Element countryBox = document.getElementById("countryBox");
    if (countryBox == null) {
      countryBox = DOM.createDiv();
      document.getBody().appendChild(countryBox);
    }
    final Element countryBoxElement = countryBox;
    conditionAndCountries(countryBox);

    getGae().ping(new Callback<String>() {
      public void onSuccess(String result) {
        countryBoxElement.getParentElement().insertAfter(new Label("GROND version 0.1").getElement(),
            countryBoxElement);
      }
    });
  }

  /** Displays the condition and countries selector, leading to the map page of that country. */
  protected void conditionAndCountries(Element div) {
    // Prepare our table.
    FlexTable table = new FlexTable();
    // Table headers.
    table.setHTML(0, 0, "<span class=\"h3\">Chronic Fatigue Syndrome</span>");
    table.setHTML(0, 1, "<span class=\"h3\">Fibromyalgia</span>");
    // Countries.
    for (Country country : Countries.COUNTRIES) {
      int rows = table.getRowCount();
      for (int col : Arrays.asList(0, 1)) {
        table.setWidget(rows, col, new Anchor(country.name, false));
        table.getCellFormatter().addStyleName(rows, col, col == 0 ? "cfsCountryList" : "fmCountryList");
        table.getCellFormatter().getElement(rows, col).getStyle().setPaddingLeft(20, Unit.PX);
      }
    }
    // Clear the working area, leaving only our table.
    div.appendChild(table.getElement());
    while (div.getChildCount() != 1)
      div.removeChild(div.getFirstChild());
    // Fix enclosing div height.
    div.getStyle().setHeight(table.getOffsetHeight() + 30, Unit.PX);
  }
}
