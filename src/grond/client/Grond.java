package grond.client;

import grond.shared.Countries;
import grond.shared.Countries.Country;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Our main class.
 */
public class Grond implements EntryPoint, ValueChangeHandler<String> {
  /** Region selector, updated via AmMap.
   * @see #rateFormInto
   * @see #amRegisterClick */
  protected static ListBox REGION = null;
  /** Panel containing the country and condition selector (in "countryBox" on the page).
   * @see #countryBox */
  protected RootPanel countries = null;
  /** The DOM element where the {@link #countries} panel is located. */
  protected Element countryBox = null;
  protected final Grond grondSelf = this;
  /** Cached information about the currently logged in user. */
  protected GwtUser currentUser = null;
  /** Maps request id to JSONP URL. Used to automatically repeat failed requests. */
  protected HashMap<String, String[]> gaeRequests = null;

  /** {@link AsyncCallback} with standard error handler. */
  public abstract class Callback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable caught) {
      serverError();
    }
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

    getGae().getCurrentUser(new Callback<GwtUser>() {
      public void onSuccess(final GwtUser user) {
        statusLine(user);
      }
    });
  }

  protected Logger getLog() {
    return Logger.getLogger("Grond");
  }

  protected Gae getGae() {
    if (gaeRequests == null) gaeRequests = new HashMap<String, String[]>();
    return new Gae(this, gaeRequests);
  }

  /** Creates a status line which shows that the GROND server is available
   * and whether the current visitor is signed in. */
  protected void statusLine(final GwtUser user) {
    this.currentUser = user;

    final RootPanel rootPanel = RootPanel.get("statusLinePlacement");

    final HTMLPanel panel = new HTMLPanel("<span id='g.version'></span>" + "<span id='g.login'></span>"
        + "<span id='g.tests'></span>");
    rootPanel.add(panel);
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
          final String href = Window.Location.getHref();
          getGae().gaeString(new Callback<String>() {
            public void onSuccess(String url) {
              getLog().info("Logout URL: " + url);
              Window.Location.replace(url);
            }
          }, "op", "createLogoutURL", "destinationURL", href);
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

      final FlowPanel updatesPanel = new FlowPanel();
      final Widget updatesFiller = new Label("GROND version 0.1");
      final Image waiting = new Image(Grond.ajaxLoaderHorisontal1());
      panel.add(updatesPanel, "g.version");
      new Timer() {
        // Wait a bit before displaying the notification.
        double pendingSeconds = 0.0;

        @Override
        public void run() {
          if (gaeRequests == null || gaeRequests.isEmpty()) {
            pendingSeconds = 0.0;
            if (updatesPanel.getWidgetCount() != 1 || updatesPanel.getWidget(0) != updatesFiller) {
              updatesPanel.clear();
              updatesPanel.add(updatesFiller);
            }
            return;
          }
          pendingSeconds += 0.1;
          if (pendingSeconds < 2.0) return;
          updatesPanel.clear();
          updatesPanel.add(waiting);
          updatesPanel.add(new InlineHTML(" Update requests pending: " + gaeRequests.size()));
        }
      }.scheduleRepeating(100);
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
            getLog().info("Yahoo login: " + url);
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

  /** Pass History events to {@link #onHistoryChange}. */
  public void onValueChange(ValueChangeEvent<String> event) {
    onHistoryChange(event.getValue());
  }

  /** Parse URL, select and render the corresponding page. */
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
      final String[] parts = parseMapOfToken(token);
      final String countryId = parts[0];
      final String condition = parts[1];
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
    } else if (token.startsWith("ratingsIn_")) { // Region page.
      initCountryBox();
      // ...
    } else if (token.startsWith("rateFormIn_")) {
      initCountryBox();
      final String[] parts = token.split("_");
      final RatingForm form = new RatingForm(grondSelf, parts[1], parts[2], parts[3], countries);
      form.addToPanel();
    }
  }

  protected static String[] parseMapOfToken(final String mapOfToken) {
    assert (mapOfToken.startsWith("mapOf_"));
    final int secondUnderscore = mapOfToken.indexOf('_', 6);
    if (secondUnderscore == -1) throw new RuntimeException("Wrong map token: " + mapOfToken);
    final String countryId = mapOfToken.substring(6, secondUnderscore);
    final String condition = mapOfToken.substring(secondUnderscore + 1);
    return new String[] { countryId, condition };
  }

  /** A form with information necessary to proceed to the rating (doctor's name and location). */
  protected void rateFormInto(final RootPanel root, final String countryId, final String condition) {
    root.add(new Label("Thanks for participating!"));
    root.add(new Label(
        "Please enter your doctor's name and location. This is necessary to recognize one doctor from another."));
    root.add(new HTML("<div id='ammap'/>"));
    final Country country = Countries.getCountry(countryId);
    ammap(country.mapFolder);

    // http://google-web-toolkit.googlecode.com/svn/javadoc/2.1/com/google/gwt/user/client/ui/ListBox.html
    REGION = new ListBox();
    for (String title : country.getRegions()) {
      REGION.addItem(title);
    }
    root.add(new InlineLabel("State: "));
    REGION.getElement().setId("dnl-region");
    root.add(REGION);
    root.add(new InlineHTML("<br/>"));

    root.add(new InlineLabel("City:"));
    final TextBox city = new TextBox();
    city.getElement().setId("dnl-city");
    root.add(city);
    root.add(new InlineHTML("<br/>"));

    root.add(new InlineLabel("Name:"));
    final TextBox name = new TextBox();
    name.getElement().setId("dnl-name");
    root.add(name);
    root.add(new InlineHTML("<br/>"));

    root.add(new InlineLabel("Surname:"));
    final TextBox surname = new TextBox();
    surname.getElement().setId("dnl-surname");
    root.add(surname);
    root.add(new InlineHTML("<br/>"));

    final Button next = new Button("Next");
    next.getElement().setId("dnl-next");
    next.setEnabled(false);

    final KeyUpHandler nextEnabler = new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        next.setEnabled(city.getValue().length() != 0 && name.getValue().length() != 0
            && surname.getValue().length() != 0);
      }
    };
    final ChangeHandler nextEnabler2 = new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        nextEnabler.onKeyUp(null);
      }
    };
    // We need both KeyUpHandler and ChangeHandler:
    // KeyUpHandler to react immediately when the field is edited with keyboard
    // and ChangeHandler to react at all if the field is changed by mouse, javascript or unit tests.
    city.addKeyUpHandler(nextEnabler);
    city.addChangeHandler(nextEnabler2);
    name.addKeyUpHandler(nextEnabler);
    name.addChangeHandler(nextEnabler2);
    surname.addKeyUpHandler(nextEnabler);
    surname.addChangeHandler(nextEnabler2);

    // TODO: Implement doctor suggestions.
//    final KeyUpHandler suggest = new KeyUpHandler() {
//      public void onKeyUp(final KeyUpEvent event) {
//        if (event.getSource() == city) {
//          final PopupPanel popup = new PopupPanel() {
//            {
//              setWidget(new Label("test"));
//            }
//          };
//          popup.show();
//        }
//      }
//    };
//    city.addKeyUpHandler(suggest);
//    name.addKeyUpHandler(suggest);
//    surname.addKeyUpHandler(suggest);

    final HTML errorMessage = new HTML("");
    errorMessage.getElement().setId("dnl-error");
    errorMessage.addStyleName("error");
    root.add(errorMessage);

    next.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        final String region = REGION.getValue(REGION.getSelectedIndex());

        // Send doctor information to the server.
        getGae().nameAndLocation(countryId, region, city.getValue(), name.getValue(), surname.getValue(),
            condition, new Callback<JSONObject>() {
              public void onSuccess(JSONObject result) {
                if (result.containsKey("errorMessage")) {
                  final String message = result.get("errorMessage").isString().stringValue();
                  errorMessage.setHTML(message);
                  Window.alert(message);
                } else {
                  final String ratingId = result.get("ratingId").isString().stringValue();
                  assert ratingId.length() > 10 : "ratingId is too short";
                  root.clear();
                  final boolean doctorCreated = result.get("doctorCreated").isBoolean().booleanValue();
                  if (doctorCreated) {
                    root.add(new Label("Thanks for telling us about this practitioner!"));
                  } else {
                    root.add(new Label("This practicioner is withing our database."
                        + " Please go on with your rating!"));
                  }
                  History.newItem("rateFormIn_" + countryId + '_' + condition + '_' + ratingId, false);
                  final RatingForm form = new RatingForm(grondSelf, countryId, condition, ratingId, root);
                  form.rating = result;
                  form.addToPanel();
                }
              }
            });
      }
    });
    root.add(next);
  }

  /** Load AmMap into `ammap` element. */
  native void ammap(String mapFolder)
  /*-{
    // Callback the AmMap will invoke when a region is clicked.
    $wnd.amRegisterClick = $entry(@grond.client.Grond::amRegisterClick(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
    // http://www.ammap.com/docs/v.2/basics/adding_map_to_a_page
    var mapArgs = {
      path: '/ammap/',
      settings_file: '/ammap/_countries/' + mapFolder + '/ammap_settings.xml',
      data_file: '/ammap/_countries/' + mapFolder + '/ammap_data.xml'
    }
    if ($doc.getElementById('ammap') == null) alert ("Internal error: 'ammap' div not found!");
    if ($wnd.swfobject == null) alert ("Internal error: swfobject not loaded!");
    // http://code.google.com/p/swfobject/wiki/documentation
    $wnd.swfobject.embedSWF ("/ammap/ammap.swf", "ammap", 800, 300, "9.0.0", null, mapArgs)
  }-*/;

  /** AmMap callback. All parameters are null except `title`, which is a region name. */
  public static void amRegisterClick(final String map_id, final String object_id, final String title,
      final String value) {
    Logger.getLogger("amRegisterClick").info("Got a click from AmMap; title: " + title);
    if (title == null || title.equals("null")) return; // Ignore clicks on empty space.
    if (REGION == null) {
      final String[] parts = parseMapOfToken(History.getToken());
      final String countryId = parts[0];
      final String condition = parts[1];
      final String safeTitle = title.replaceAll("[\\W\\_\\s]+", "");
      History.newItem("ratingsIn_" + countryId + '_' + safeTitle + '_' + condition);
    } else {
      for (int i = 0; i < REGION.getItemCount(); ++i) {
        final String text = REGION.getItemText(i);
        if (text.equals(title)) REGION.setItemSelected(i, true);
      }
    }
  }

  /** Displays the condition and countries selector, leading to the map page of that country. */
  protected void conditionAndCountries() {
    // Prepare our table.
    final FlexTable table = new FlexTable();
    table.getElement().setId("countryTable"); // For automatic testing.
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
    topDoctors.getElement().setId("topDoctors"); // For automatic testing.
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
        getGae().getCurrentUser(new Callback<GwtUser>() {
          public void onSuccess(GwtUser user) {
            currentUser = user;
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

    countries.add(new HTML("<div id='ammap'/>"));
    ammap(country.mapFolder);

    // Make sure we have space for all the doctors and the login form.
    countryBox.getStyle().setHeight(countries.getOffsetHeight() + 70, Unit.PX);
  }

  /** Checks if the expression holds and if not throws the error code.
   * To generate an error code:
   * <pre>perl -e 'print int (rand (99999999)) . "\n"'</pre> */
  protected void affirm(boolean expression, int errorCode) {
    if (expression) return;
    getLog().severe("Assertion failed! Error code: " + errorCode);
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

  /** Img src of images/ajax-loader-horisontal-1.gif */
  static String ajaxLoaderHorisontal1() {
    return GWT.getHostPageBaseURL() + "images/ajax-loader-horisontal-1.gif";
  }
}
