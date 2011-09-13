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
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
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
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
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
  protected static final char[] HEX_ENCODING_TABLE = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
      'B', 'C', 'D', 'E', 'F' };
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
    try {
      onHistoryChange(History.getToken());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

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
      final Widget updatesFiller = new Label("GROND version 0.2");
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

  protected FlowPanel loginForm() {
    final FlowPanel panel = new FlowPanel();

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
    }));

    panel.add(new Button("Sign in with Yahoo", new ClickHandler() {
      public void onClick(ClickEvent event) {
        getGae().createLoginURL(href, host, "http://yahoo.com/", new Callback<String>() {
          public void onSuccess(String url) {
            getLog().info("Yahoo login: " + url);
            Window.Location.replace(url);
          }
        });
      }
    }));

    panel.add(new Label("OpenID:"));
    final TextBox openid = new TextBox();
    panel.add(openid);
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
    }));

    return panel;
  }

  /** Pass History events to {@link #onHistoryChange}. */
  public void onValueChange(ValueChangeEvent<String> event) {
    try {
      onHistoryChange(event.getValue());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Parse URL, select and render the corresponding page. */
  protected void onHistoryChange(String token) throws Exception {
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
    } else if (token.startsWith("mapOf_") || token.startsWith("ratingsIn_")) { // Country/Region page.
      final String[] parts = token.split("\\_");
      final String countryId = parts[1];
      final String condition = parts[2];
      final String safeRegionId = parts.length >= 4 ? parts[3] : null;
      final String regionId = historyUnescape(safeRegionId);
      for (Country country : Countries.COUNTRIES) {
        if (countryId.equals(country.id)) {
          initCountryBox();
          countryOrRegionInfo(country, regionId, condition);
        }
      }
    } else if (token.startsWith("rateIn_")) { // Add rating for country.
      final String[] parts = token.split("\\_");
      final String countryId = parts[1];
      final String condition = parts[2];
      final String safeRegionId = parts.length >= 4 ? parts[3] : null;
      final String regionId = historyUnescape(safeRegionId);
      initCountryBox();
      rateFormInto(countries, countryId, regionId, condition);
    } else if (token.startsWith("rateFormIn_")) {
      initCountryBox();
      final String[] parts = token.split("_");
      final RatingForm form = new RatingForm(grondSelf, parts[1], parts[2], parts[3], countries);
      form.addToPanel();
    } else if (token.startsWith("ptrp_")) {
      initCountryBox();
      int doctorId = Integer.parseInt(token.substring("ptrp_".length()));
      final PractitionerTRP treatmentReviewPage = new PractitionerTRP(grondSelf, countries, doctorId, null);
      treatmentReviewPage.addToPanel();
    }
  }

  /** A form with information necessary to proceed to the rating (doctor's name and location). */
  protected void rateFormInto(final RootPanel root, final String countryId, final String regionId,
      final String condition) throws Exception {
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
      if (regionId != null && title.equals(regionId)) REGION.setSelectedIndex(REGION.getItemCount() - 1);
    }
    root.add(new InlineLabel("State: "));
    REGION.getElement().setId("dnl-region");
    root.add(REGION);
    root.add(new InlineHTML("<br/>"));

    root.add(new InlineLabel("City:"));
    final MultiWordSuggestOracle cityOracle = new MultiWordSuggestOracle();
    final MultiWordSuggestOracle nameOracle = new MultiWordSuggestOracle();
    final SuggestBox city = new SuggestBox(cityOracle);
    final ChangeHandler regionSuggestionsHandler = new ChangeHandler() {
      @Override
      public void onChange(final ChangeEvent event) {
        city.setValue("");

        final String region = REGION.getItemText(REGION.getSelectedIndex());
        getGae().getDoctorSuggestions(region, null, new Callback<JSONObject>() {
          @Override
          public void onSuccess(final JSONObject suggestions) {
            final JSONArray cities = suggestions.get("cities").isArray();
            cityOracle.clear();
            for (int index = 0; index < cities.size(); ++index)
              cityOracle.add(cities.get(index).isString().stringValue());

            final JSONArray names = suggestions.get("names").isArray();
            nameOracle.clear();
            for (int index = 0; index < names.size(); ++index)
              nameOracle.add(names.get(index).isString().stringValue());
          }
        });
      }
    };
    regionSuggestionsHandler.onChange(null); // Populate `cityOracle`.
    REGION.addChangeHandler(regionSuggestionsHandler); // Repopulate `cityOracle` when `REGION` changes.
    city.getElement().setId("dnl-city");
    root.add(city);
    root.add(new InlineHTML("<br/>"));

    final SuggestBox name = new SuggestBox(nameOracle);
    final SuggestBox surname = new SuggestBox(nameOracle);
    final SelectionHandler<Suggestion> nameSuggestionHandler = new SelectionHandler<Suggestion>() {
      @Override
      public void onSelection(final SelectionEvent<Suggestion> event) {
        final String selected = event.getSelectedItem().getReplacementString();
        final int space = selected.indexOf(' ');
        final int comma = selected.indexOf(',');
        if (space > 0 && comma > 0 && space < comma) {
          name.setValue(selected.substring(0, space));
          surname.setValue(selected.substring(space + 1, comma));
          city.setValue(selected.substring(comma + 1));
        }
      }
    };
    name.addSelectionHandler(nameSuggestionHandler);
    surname.addSelectionHandler(nameSuggestionHandler);

    root.add(new InlineLabel("Name:"));
    name.getElement().setId("dnl-name");
    root.add(name);
    root.add(new InlineHTML("<br/>"));

    root.add(new InlineLabel("Surname:"));
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
    city.getTextBox().addChangeHandler(nextEnabler2);
    name.addKeyUpHandler(nextEnabler);
    name.getTextBox().addChangeHandler(nextEnabler2);
    surname.addKeyUpHandler(nextEnabler);
    surname.getTextBox().addChangeHandler(nextEnabler2);

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

                  // Refresh the doctor's list.
                  getGae().cleanCache("^getDoctorsByRating");
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
    else if ($wnd.swfobject == null) alert ("Internal error: swfobject not loaded!");
    // http://code.google.com/p/swfobject/wiki/documentation
    else $wnd.swfobject.embedSWF ("/ammap/ammap.swf", "ammap", 800, 300, "9.0.0", null, mapArgs)
  }-*/;

  /** AmMap callback. All parameters are null except `title`, which is a region name. */
  public static void amRegisterClick(final String map_id, final String object_id, final String title,
      final String value) throws Exception {
    Logger.getLogger("amRegisterClick").info("Got a click from AmMap; title: " + title);
    if (title == null || title.equals("null")) return; // Ignore clicks on empty space.
    if (REGION == null) {
      final String[] parts = History.getToken().split("\\_");
      final String countryId = parts[1];
      final String condition = parts[2];
      final String safeTitle = historyEscape(title);
      History.newItem("ratingsIn_" + countryId + '_' + condition + '_' + safeTitle);
    } else {
      for (int i = 0; i < REGION.getItemCount(); ++i) {
        final String text = REGION.getItemText(i);
        if (text.equals(title)) {
          REGION.setItemSelected(i, true);
          REGION.fireEvent(new ChangeEvent() {
          });
        }
      }
    }
  }

  /** We use '_' as a separator in history tokens and we need to escape it. */
  protected static String historyEscape(final String part) throws Exception {
    final StringBuilder sb = new StringBuilder(part.length());
    for (int i = 0; i < part.length(); ++i) {
      final char ch = part.charAt(i);
      if (Character.isLetterOrDigit(ch)) {
        sb.append(ch);
      } else {
        final String chs = part.substring(i, i + 1);
        final byte[] bytes = chs.getBytes("UTF-8");
        for (int j = 0; j < bytes.length; ++j) {
          int v = bytes[j] & 0xff;
          sb.append('!');
          sb.append(HEX_ENCODING_TABLE[(v >>> 4)]);
          sb.append(HEX_ENCODING_TABLE[v & 0xf]);
        }
      }
    }
    return sb.toString();
  }

  /** @see #historyEscape */
  protected static String historyUnescape(final String part) throws Exception {
    if (part == null) return null;
    final byte[] bytes = new byte[part.length() + 4];
    int end = 0;
    for (int i = 0; i < part.length(); ++i) {
      final int ch = part.charAt(i);
      if (ch != '!' || part.length() - i < 3) {
        final String chs = part.substring(i, i + 1);
        final byte[] chb = chs.getBytes("UTF-8");
        System.arraycopy(chb, 0, bytes, end, chb.length);
        end += chb.length;
      } else {
        bytes[end++] = (byte) (Integer.parseInt(part.substring(i + 1, i + 3), 16) & 0xFF);
        i += 2;
      }
    }
    return new String(bytes, 0, end, "UTF-8");
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

  /** Show user that the button has been clicked: make it gray, make it live. */
  protected Timer buttonInProgressStart(final Button button) {
    button.setEnabled(false);
    final Timer timer = new Timer() {
      final String originalLabel = button.getHTML();
      int counter = 0;

      @Override
      public void run() {
        if (button.isEnabled()) {
          // Restore the label after this timer is cancelled. See `buttonInProgressEnd`.
          button.setHTML(originalLabel);
        } else {
          button.setHTML(originalLabel + " <span style=\"font-family: monospace\">"
              + (counter % 3 == 0 ? ".&nbsp;&nbsp;" : counter % 3 == 1 ? "..&nbsp;" : "...") + "</span>");
        }
        ++counter;
      }
    };
    timer.scheduleRepeating(500);
    return timer;
  }

  protected void buttonInProgressEnd(final Button button, final Timer timer) {
    timer.cancel();
    button.setEnabled(true);
    timer.run(); // Restore the button label.
  }

  /** Puts doctor's rating and map into {@link #countryBox}.
   * @param regionId is null for country pages. */
  protected void countryOrRegionInfo(final Country country, final String regionId, final String condition)
      throws Exception {
    countries.add(new Label("Condition: " + condition));
    countries.add(new Label("Rating for " + country.name));
    if (regionId != null) countries.add(new Label("Region: " + regionId));

    countries.add(new HTML("<br/>"));

    final FlexTable topDoctors = new FlexTable();
    topDoctors.getElement().setId("topDoctors"); // For automatic testing.
    countries.add(topDoctors);
    topDoctors.setWidget(0, 0, new Image(ajaxLoaderHorisontal1()));
    final Callback<JSONArray> renderDoctors = new Callback<JSONArray>() {
      @Override
      public void onSuccess(final JSONArray doctors) {
        topDoctors.setHTML(0, 0, "Name of practitioner");
        topDoctors.setHTML(0, 1, "Type of practitioner<br/>(most people said...)");
        topDoctors.setHTML(0, 2, "City");
        topDoctors.setHTML(0, 3, country.id == "usa" ? "State" : "Region");
        topDoctors.setHTML(0, 4, "# of Reviews");
        topDoctors.setHTML(0, 5, "Average Cost");
        topDoctors.setHTML(0, 6, "Average Satisfaction");
        topDoctors.setHTML(0, 7, "Experience");

        for (int di = 0; di < doctors.size(); ++di) {
          final JSONObject doctor = doctors.get(di).isObject();
          if (doctor == null) continue;
          final String firstName = doctor.get("firstName").isString().stringValue();
          final String lastName = doctor.get("lastName").isString().stringValue();
          final Anchor name = new Anchor(firstName + " " + lastName);
          final JSONValue doctorId = doctor.get("_id");
          if (doctorId != null) {
            final int id = (int) doctorId.isNumber().doubleValue();
            name.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                // Open the "Practitioner Treatment Review Page".
                History.newItem("ptrp_" + id, false);
                initCountryBox();
                final PractitionerTRP treatmentReviewPage = new PractitionerTRP(grondSelf, countries, id,
                    doctor);
                treatmentReviewPage.addToPanel();
              }
            });
          }
          topDoctors.setWidget(1 + di, 0, name);

          final JSONValue typeObject = doctor.get("_type"); // Types sorted by count in ratings.
          final JSONArray type = typeObject != null ? typeObject.isArray() : null;
          if (type != null && type.size() != 0) {
            String typeString = type.get(0).isString().stringValue();
            //if (type.size() > 2) typeString += ", " + type.get(2).isString().stringValue();
            topDoctors.setHTML(1 + di, 1, typeString);
          }

          topDoctors.setHTML(1 + di, 2, doctor.get("city").isString().stringValue());
          topDoctors.setHTML(1 + di, 3, doctor.get("region").isString().stringValue());
          final JSONValue numberOfReviews = doctor.get("_numberOfReviews");
          if (numberOfReviews != null) topDoctors.setHTML(1 + di, 4,
              Integer.toString((int) numberOfReviews.isNumber().doubleValue()));

          final JSONValue averageCostLevel = doctor.get("_averageCostLevel");
          if (averageCostLevel != null && averageCostLevel.isNumber() != null) {
            final int acl = (int) averageCostLevel.isNumber().doubleValue();
            final StringBuilder html = new StringBuilder();
            for (int lev = 1; lev <= 5; ++lev) {
              final String clazz = lev <= acl ? "AverageCostStrong" : "AverageCostWeak";
              html.append("<span class=\"").append(clazz).append("\">$</span>");
            }
            topDoctors.setHTML(1 + di, 5, html.toString());
          }

          final JSONValue satisfaction = doctor.get("_" + condition + "Satisfaction");
          if (satisfaction != null) topDoctors.setHTML(1 + di, 6,
              Integer.toString((int) satisfaction.isNumber().doubleValue()));

          final JSONValue experience = doctor.get("_experience");
          if (experience != null) topDoctors.setHTML(1 + di, 7, experience.isString().stringValue());

          String rateLabel = "Rate!";
          final JSONValue fromCurrentUser = doctor.get("_fromCurrentUser");
          if (fromCurrentUser != null) {
            if (fromCurrentUser.isString().stringValue() == "finished") rateLabel = "Update";
            else rateLabel = "Finish";
          }
          // Change the rate button label if there exists a rating for that doctor.

          final Button rate = new Button(rateLabel);
          final int currentRow = 1 + di;
          rate.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
              if (currentUser == null) {
                rate.setEnabled(false);
                topDoctors.setWidget(currentRow, 4, loginForm());
              } else {
                final Timer inProgress = buttonInProgressStart(rate);
                // Get/create a rating for this doctor.
                final String countryId = doctor.get("country").isString().stringValue();
                getGae().nameAndLocation(countryId, doctor.get("region").isString().stringValue(),
                    doctor.get("city").isString().stringValue(),
                    doctor.get("firstName").isString().stringValue(),
                    doctor.get("lastName").isString().stringValue(), condition, new Callback<JSONObject>() {
                      @Override
                      public void onSuccess(final JSONObject result) {
                        inProgress.cancel();
                        if (result.containsKey("errorMessage")) {
                          final String message = result.get("errorMessage").isString().stringValue();
                          Window.alert(message);
                        } else {
                          initCountryBox();
                          final String ratingId = result.get("ratingId").isString().stringValue();
                          assert ratingId.length() > 10 : "ratingId is too short";
                          History
                              .newItem("rateFormIn_" + countryId + '_' + condition + '_' + ratingId, false);
                          final RatingForm form = new RatingForm(grondSelf, countryId, condition, ratingId,
                              countries);
                          form.rating = result;
                          form.addToPanel();
                        }
                      }
                    });
              }
            }
          });
          topDoctors.setWidget(1 + di, 8, rate);
        }
      }
    };
    getGae().getDoctorsByRating(country, regionId, condition, 10, renderDoctors);

    countries.add(new HTML("<br/>"));

    final Button addDoc = new Button("Haven't found your doctor in the list? Click here to add!");
    addDoc.addClickHandler(new ClickHandler() {
      boolean loginIsVisible = false;

      public void onClick(ClickEvent event) {
        if (loginIsVisible) return;
        if (currentUser == null) {
          int widgetIndex = countries.getWidgetIndex(addDoc);
          countries.remove(widgetIndex);
          countries.insert(new Label("Not signed in! Please sign in to add the doctor."), widgetIndex);
          countries.insert(loginForm(), widgetIndex + 1);
        } else {
          try {
            History.newItem("rateIn_" + country.id + "_" + condition + "_"
                + (regionId != null ? historyEscape(regionId) : ""), true);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
        loginIsVisible = true;
      }
    });
    countries.add(addDoc);

    countries.add(new HTML("<br/>"));

    if (regionId == null) {
      countries.add(new HTML("<div id='ammap'/>"));
      ammap(country.mapFolder);
    }

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
