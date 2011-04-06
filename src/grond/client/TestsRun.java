package grond.client;

import java.util.Arrays;
import java.util.LinkedList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

public class TestsRun {
  protected static TestsRun INSTANCE;

  public static TestsRun getInstance() {
    if (INSTANCE == null) INSTANCE = new TestsRun();
    return INSTANCE;
  }

  protected Panel testsPanel;
  protected int warmCounter = 0;

  protected TestsRun() {}

  /** {@link AsyncCallback} with standard error handler. */
  public abstract class Callback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable caught) {
      RootPanel page = RootPanel.get();
      page.add(new HTML("An error occurred while "
          + "attempting to contact the server.<br/>\nPlease check your network "
          + "connection and try again.<br/>\n" + caught));
    }
  }

  /** Invokes a "tests" servlet returning a string. */
  void testsString(AsyncCallback<String> callback, String... parameters) {
    final JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
    jsonp.setTimeout(40000); // Default TCP/IP timeout + 10 seconds.

    String url = GWT.getModuleBaseURL() + "tests";
    for (int i = 0; i < parameters.length - 1; i += 2) {
      url += (i == 0 ? '?' : '&') + URL.encodeQueryString(parameters[i]) + '='
          + URL.encodeQueryString(parameters[i + 1]);
    }

    jsonp.requestString(url, callback);
  }

  protected Panel getTestsPanel() {
    if (testsPanel != null) return testsPanel;

    final FlexTable table = new FlexTable();
    final ScrollPanel scroll = new ScrollPanel(table);
    // getScrollHeight currently returns 0 on Google Chrome 7, therefore we protect it with Math.max.
    final int scrollHeight = Math.max(500, Document.get().getScrollHeight());
    scroll.setHeight((scrollHeight * 90 / 100) + "px");

    final LinkedList<String> tests = new LinkedList<String>(Arrays.asList("UEDNL", "VCSRLODIC1", "VCSRLODIC2",
        "SVCCC"));

    final HTML internalTests = new HTML("warming up");
    table.setWidget(0, 0, new Label("Internal tests:"));
    table.setWidget(1, 0, internalTests);
    table.getFlexCellFormatter().setColSpan(1, 0, 3);

    // Warming cycle. Keep several application instances online.
    final Timer warmingTimer = new Timer() {
      boolean internalTestsStarted = false;

      @Override
      public void run() {
        // Slow down when all tests have fired.
        if (tests.isEmpty()) this.schedule(10000);
        else this.schedule(2000 / tests.size());

        testsString(new Callback<String>() {
          public void onSuccess(String result) {
            warmCounter += 1;
          }
        }, "gaeSpinUp", "true");

        if (warmCounter >= 3 && !internalTestsStarted) {
          // Run internal tests after spinUp.
          internalTestsStarted = true;
          internalTests.setHTML("running");
          testsString(new Callback<String>() {
            public void onSuccess(String result) {
              internalTests.setHTML(result);
            }
          }, "internalTests", "true");
        }
      }
    };
    warmingTimer.run();
    warmingTimer.run();
    warmingTimer.schedule(200);

    final Timer fireTimer = new Timer() {
      @Override
      public void run() {
        final int row = table.getRowCount();
        if (tests.isEmpty()) {
          this.cancel();
          table.setWidget(row, 0, new Label("End."));
        } else {
          final String test = tests.removeFirst();

          table.setWidget(row, 0, new InlineLabel(test + " .. "));
          final InlineHTML status = new InlineHTML("");
          final ScrollPanel statusScroll = new ScrollPanel(status);
          table.setWidget(row, 1, statusScroll);

          /** Pointer to the "runTest" ClickHandler. */
          final LinkedList<ClickHandler> runTestPtr = new LinkedList<ClickHandler>();

          /** Checks and displays the test result (pass or fail). */
          final AsyncCallback<String> callback = new AsyncCallback<String>() {
            protected int timeouts = 0;

            public void onFailure(Throwable caught) {
              status.setHTML("<span style=\"color: red\">RPC failure<br/>\n" + caught + "</span>.");
            }

            public void onSuccess(String result) {
              if (result != null && result.equals("okay")) {
                status.setHTML("<span style=\"color: green\">pass</span>.");
                statusScroll.getElement().getStyle().clearWidth();
                statusScroll.getElement().getStyle().clearHeight();
              } else if (timeouts < 2 && result.contains("Timeout while fetching")) { // Repeat the test.
                ++timeouts;
                runTestPtr.get(0).onClick(null);
              } else {
                status.setHTML("<pre style=\"color: red; font-size: smaller\">fail!\n"
                    + result.replaceAll("\\<", "&lt;") + "</pre>");
                if (status.getOffsetWidth() > Document.get().getScrollWidth() * 70 / 100
                    || status.getOffsetHeight() > scrollHeight * 70 / 100) {
                  statusScroll.setWidth((Document.get().getScrollWidth() * 70 / 100) + "px");
                  statusScroll.setHeight((scrollHeight * 70 / 100) + "px");
                } else {
                  statusScroll.getElement().getStyle().clearWidth();
                  statusScroll.getElement().getStyle().clearHeight();
                }
              }
            }
          };

          final ClickHandler runTest = new ClickHandler() {
            public void onClick(ClickEvent event) {
              final boolean autoRestart = (event == null);
              // Do not restart the test if it isn't finished yet.
              if (!autoRestart && status.getHTML().contains("⌚")) {
                status.setHTML("already! " + status.getHTML());
                return;
              }

              // Show that we started the test.
              status.setHTML("⌚");

              testsString(callback, "test", test);
            }
          };
          runTestPtr.add(runTest);

          final Button repeatButton = new Button("repeat");
          table.setWidget(row, 2, repeatButton);
          repeatButton.addClickHandler(runTest);

          // Run the test immediately.
          runTest.onClick(null);
        }
      }
    };
    fireTimer.scheduleRepeating(1000);

    testsPanel = scroll;
    return testsPanel;
  }

  protected void testInto(RootPanel root) {
    root.getElement().getStyle().clearHeight();
    root.add(getTestsPanel());
  }
}
