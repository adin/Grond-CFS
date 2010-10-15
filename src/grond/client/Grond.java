package grond.client;

import grond.shared.Countries;
import grond.shared.Countries.Country;

import java.util.Arrays;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.maddison.gwt.logging.client.Logger;
import com.maddison.gwt.logging.client.Logging;

/**
 * Our main class.
 */
public class Grond implements EntryPoint, ValueChangeHandler {
  /** {@link AsyncCallback} with standard error handler. */
  public abstract class Callback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable caught) {
      serverError();
    }
  }

  protected Logger log = null;
  protected GaeAsync gae = null;
  /** Panel containing the country and condition selector (in "countryBox" on the page).
   * @see #countryBox */
  protected RootPanel countries = null;
  /** The DOM element where the {@link #countries} panel is located. */
  protected Element countryBox = null;

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
    History.addValueChangeHandler(this);
    onHistoryChange(History.getToken());

    getGae().getCurrentUser(new Callback<Gae.GwtUser>() {
      public void onSuccess(Gae.GwtUser user) {
        statusLine(user);
      }
    });
  }

  /** Creates a status line which shows that the GROND server is available
   * and whether the current visitor is signed in. */
  protected void statusLine(Gae.GwtUser user) {
    // Add status line after country box.
    // RootPanel should be at the root of any GWT hierarchy in order for the GWT events to work.
    Element div = DOM.createDiv();
    div.setId("g.header.div");
    countryBox.getParentElement().insertAfter(div, countryBox);
    RootPanel rootPanel = RootPanel.get("g.header.div");

    final HTMLPanel panel = new HTMLPanel(
        "<span id='g.version'></span><span id='g.login'></span><span id='g.tests'></span>");
    rootPanel.add(panel);
    panel.add(new Label("GROND version 0.1"), "g.version");
    if (user == null) {
      panel.add(new Label("You are not currently signed in!"), "g.login");
      final Anchor signIn = new Anchor("Sign in");
      signIn.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          removeChildren(panel.getElementById("g.login"), 0);
          panel.add(loginForm(), "g.login");
        }
      });
      panel.add(signIn, "g.login");
    } else {
      panel.add(new Label("Hi, you are signed in as " + user.email + "!"), "g.login");
      final Anchor signOut = new Anchor("Sign out<br/>", true);
      signOut.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          String href = Window.Location.getHref();
          getGae().createLogoutURL(href, new Callback<String>() {
            public void onSuccess(String url) {
              getLog().info("Logout URL: " + url);
              Window.Location.replace(url);
            }
          });
        }
      });
      panel.add(signOut, "g.login");
      final Anchor test = new Anchor("test!");
      test.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          History.newItem("test_1mmheumpihd6h", true); // Secure from non-admin users.
        }
      });
      if (user.isAdmin) panel.add(test, "g.tests");
    }
  }

  protected HTMLPanel loginForm() {
    HTMLPanel panel = new HTMLPanel(
        "<span id='g.loginFormButtons'></span><br/><span id='g.loginFormInput'></span>");

    final String href = Window.Location.getHref();
    final String host = Window.Location.getHostName();

    panel.add(new Button("Sign in with Google", new ClickHandler() {
      public void onClick(ClickEvent event) {
        getGae().createLoginURL(href, host, "https://www.google.com/accounts/o8/id", new Callback<String>() {
          public void onSuccess(String url) {
            getLog().info("Google login: " + url);
            Window.Location.replace(url);
          }
        });
      }
    }), "g.loginFormButtons");

    panel.add(new Button("Sign in with Yahoo", new ClickHandler() {
      public void onClick(ClickEvent event) {
        getGae().createLoginURL(href, host, "http://yahoo.com/", new Callback<String>() {
          public void onSuccess(String url) {
            getLog().info("Google login: " + url);
            Window.Location.replace(url);
          }
        });
      }
    }), "g.loginFormButtons");

    panel.add(new Label("OpenID:"), "g.loginFormInput");
    final TextBox openid = new TextBox();
    panel.add(openid, "g.loginFormInput");
    panel.add(new Button("Sign in", new ClickHandler() {
      public void onClick(ClickEvent event) {
        getLog().info("OpenID login: " + openid.getText());
        getLog().info("href: " + href + "; host: " + host);
        getGae().createLoginURL(href, host, openid.getText(), new Callback<String>() {
          public void onSuccess(String url) {
            getLog().info("OpenID login URL: " + url);
            Window.Location.replace(url);
          }
        });
      }
    }), "g.loginFormInput");

    return panel;
  }

  public void onValueChange(ValueChangeEvent event) {
    if (event.getValue() instanceof String) onHistoryChange((String) event.getValue());
  }

  protected void onHistoryChange(String token) {
    // Make sure the "countryBox" container is present on the page.
    final Document document = Document.get();
    countryBox = document.getElementById("countryBox");
    if (countryBox == null) {
      countryBox = document.getBody().appendChild(document.createDivElement());
      countryBox.setAttribute("id", "countryBox");
      affirm(document.getElementById("countryBox") == countryBox, 44413824);
    }

    if (token.length() == 0) {
      conditionAndCountries();
    } else if (token.equals("test_1mmheumpihd6h")) { // Unit tests.
      initCountryBox();
      TestsRun.getInstance().testInto(countries);
    } else if (token.startsWith("mapOf_")) { // Country page.
      int secondUnderscore = token.indexOf('_', 6);
      if (secondUnderscore == -1) throw new RuntimeException("Wrong map token: " + token);
      String countryId = token.substring(6, secondUnderscore);
      String condition = token.substring(secondUnderscore + 1);
      for (Country country : Countries.COUNTRIES) {
        if (countryId.equals(country.id)) {
          initCountryBox();
          countryInfo(country, condition);
        }
      }
    } else if (token.startsWith("rateIn_")) { // Add rating for country.
      int secondUnderscore = token.indexOf('_', 7);
      if (secondUnderscore == -1) throw new RuntimeException("Wrong map token: " + token);
      String countryId = token.substring(7, secondUnderscore);
      String condition = token.substring(secondUnderscore + 1);
      initCountryBox();
      rateFormInto(countries, countryId, condition);
    }
  }

  protected void rateFormInto(RootPanel root, String countryId, String condition) {
    root.add(new Label("Thanks for participating!"));
    root.add(new Label(
        "Please enter your doctor's name and location. This is necessary to recognize one doctor from another."));
    // TODO: Get data necessary for doctorNameAndLocation.getRating.
  }

  /** Displays the condition and countries selector, leading to the map page of that country. */
  protected void conditionAndCountries() {
    // Prepare our table.
    final FlexTable table = new FlexTable();
    table.getElement().setId("countryTable"); // For Selenium testing.
    // Table headers.
    table.setHTML(0, 0, "<span class=\"h3\">Chronic Fatigue Syndrome</span>");
    table.setHTML(0, 1, "<span class=\"h3\">Fibromyalgia</span>");
    // Countries.
    for (Country country : Countries.COUNTRIES) {
      int rows = table.getRowCount();
      for (int col : Arrays.asList(0, 1)) {
        table.setWidget(rows, col, new Anchor(country.name, false));
        table.getCellFormatter().addStyleName(rows, col, col == 0 ? "cfsCountryList" : "fmCountryList");
        Element element = table.getCellFormatter().getElement(rows, col);
        element.getStyle().setPaddingLeft(20, Unit.PX);
        element.setTitle((col == 0 ? "CFS" : "FM") + " patients, click here to see a list of " + country.name
            + " doctors.");
      }
    }
    // Handle country clicks.
    table.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        HTMLTable.Cell cell = table.getCellForEvent(event);
        if (cell != null && cell.getRowIndex() > 0) {
          final Country country = Countries.COUNTRIES.get(cell.getRowIndex() - 1);
          final String condition = cell.getCellIndex() == 0 ? "cfs" : "fm";

          History.newItem("mapOf_" + country.id + "_" + condition, true);
        }
      }
    });
    // Clear the working area, leaving only our table.
    initCountryBox();
    countries.add(table);
    // Fix enclosing div height.
    countryBox.getStyle().setHeight(table.getOffsetHeight() + 30, Unit.PX);
  }

  /** Puts country rating and map into {@link #countryBox}. */
  protected void countryInfo(final Country country, final String condition) {
    countries.add(new Label("Condition: " + condition));
    countries.add(new Label("Rating for " + country.name));

    countries.add(new HTML("<br/>"));

    final FlexTable topDoctors = new FlexTable();
    countries.add(topDoctors);
    topDoctors.setHTML(0, 0, "Name");
    topDoctors.setHTML(0, 1, country.id == "usa" ? "State" : "Region");
    topDoctors.setHTML(0, 2, "City");
    topDoctors.setHTML(0, 3, "Rating");
    topDoctors.setHTML(1, 0, "not implemented");
    topDoctors.setHTML(1, 1, "not implemented");
    topDoctors.setHTML(1, 2, "not implemented");
    topDoctors.setHTML(1, 3, "not implemented");
    topDoctors.setWidget(1, 4, new Button("Rate!"));

    countries.add(new HTML("<br/>"));

    final Button addDoc = new Button("Haven't found your doctor in the list? Click here to add!");
    addDoc.addClickHandler(new ClickHandler() {
      boolean loginIsVisible = false;

      public void onClick(ClickEvent event) {
        if (loginIsVisible) return;
        getGae().getCurrentUser(new Callback<Gae.GwtUser>() {
          public void onSuccess(Gae.GwtUser user) {
            if (user == null) {
              int widgetIndex = countries.getWidgetIndex(addDoc);
              countries.remove(widgetIndex);
              countries.insert(new Label("Not signed in! Please sign in to add the doctor."), widgetIndex);
              countries.insert(loginForm(), widgetIndex + 1);
            } else {
              History.newItem("rateIn_" + country.id + "_" + condition, true);
            }
          }
        });
        loginIsVisible = true;
      }
    });
    countries.add(addDoc);

    countries.add(new HTML("<br/>"));

    countries.add(new Label("Map for " + country.name));

    // Make sure we have space for all the doctors and the login form.
    countryBox.getStyle().setHeight(countries.getOffsetHeight() + 70, Unit.PX);
  }

  /** Checks if the expression holds and if not throws the error code.
   * To generate an error code:
   * <pre>perl -e 'print int (rand (99999999)) . "\n"'</pre> */
  protected void affirm(boolean expression, int errorCode) {
    if (expression) return;
    getLog().error("Assertion failed! Error code: " + errorCode);
    throw new RuntimeException("Assertion failed! Error code: " + errorCode);
  }

  /** Remove all elements from the "countryBox" container except the RootPanel there. */
  protected void initCountryBox() {
    if (countries == null) {
      countries = RootPanel.get("countryBox");
      affirm(countries != null && countryBox != null, 84494814);
      removeChildren(countryBox, 1);
    } else {
      countries.clear();
    }
  }

  /** Remove all children elements except the last <code>tail</code> from the given DOM container. */
  protected void removeChildren(Element element, int tail) {
    while (element.getChildCount() > tail)
      element.removeChild(element.getFirstChild());
  }
}
