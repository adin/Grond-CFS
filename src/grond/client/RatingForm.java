package grond.client;

import grond.shared.DoctorRating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.CommandCanceledException;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Collect practicioner information from a user.
 */
public class RatingForm {
  final Grond grond;
  final String countryId;
  final String condition;
  final String ratingId;
  final Panel panel;
  /** If you have the rating, set it there to save us from the RPC roundtrip. */
  public DoctorRating rating;
  int step = 1;

  RatingForm(Grond grond, String countryId, String condition, String ratingId, Panel rootPanel) {
    this.grond = grond;
    this.countryId = countryId;
    this.condition = condition;
    this.ratingId = ratingId;
    this.panel = new FlowPanel();
    rootPanel.add(this.panel);
  }

  void addToPanel() {
    final HTML doctorInfo = new HTML();
    if (rating == null) doctorInfo.setHTML("<img src='" + Grond.ajaxLoaderHorisontal1() + "' />");
    panel.add(doctorInfo);

    final AsyncCallback<DoctorRating> render = new AsyncCallback<DoctorRating>() {
      public void onFailure(Throwable cause) {
        Window.alert(cause.toString());
      }

      public void onSuccess(final DoctorRating $rating) {
        assert ($rating.getFullId().equals(ratingId));
        rating = $rating;

        // Security check: signed in user should be the same as the rating's owner.
        if (grond.currentUser == null) {
          doctorInfo.setHTML("Please sign in.");
          throw new RuntimeException("Not signed in.");
        }
        final String gaeUser = grond.currentUser.getId();
        if (gaeUser == null || !gaeUser.equals(rating.user)) {
          doctorInfo.setHTML("Internal error: This rating belongs to a different user."
              + " Make sure you are signed in properly.");
          throw new RuntimeException("Expected user: " + rating.user + "; got: " + gaeUser);
        }

        assert (rating._doctor != null);
        final String name = rating._doctor.firstName;
        final String surname = rating._doctor.lastName;
        doctorInfo.setHTML("Rating of: " + name + ' ' + surname + ".");

        if (step == 1) firstStep();
        else if (step == 2) secondStep();
        else if (step == 3) thirdStep();
        else if (step == 4) fourthStep();
        else if (step == 5) finish();
        else throw new RuntimeException("Unknown step: " + step);
      }
    };

    final ValueChangeHandler<Object> continueWithUser = new ValueChangeHandler<Object>() {
      @Override public void onValueChange(ValueChangeEvent<Object> event) {
        // Reuse the rating if it is already fetched.
        if (rating != null) render.onSuccess(rating);
        else grond.getGae().getRating(ratingId, render);
      }
    };

    // We need the current user to work with the ratings. Wait for it if it isn't yet available.
    if (grond.currentUser == null) new Timer() {
      double seconds = 0.0;

      @Override public void run() {
        if (grond.currentUser != null) {
          continueWithUser.onValueChange(null);
          return;
        }
        if (seconds > 5.0) {
          doctorInfo.setHTML("Please sign in.");
          return;
        }
        Logger.getLogger("RatingForm").info("Waiting for Gae.getCurrentUser to complete.");
        seconds += 0.2;
        schedule(200);
      }
    }.run();
    else {
      continueWithUser.onValueChange(null);
    }
  }

  protected void firstStep() {
    // Save condition into the rating.
    if (!condition.equals("fm") && !condition.equals("cfs")) throw new RuntimeException("Unknown condition: "
        + condition);
    if (rating.condition == null || rating.condition.length() == 0) ratingUpdateString("condition", condition);

    panel.add(new HTML("TYPE OF HEALTH PROFESSIONAL"));
    panel.add(ratingBox("type", "Family Physician / Internist",
        "Family Physician / General Internal Medicine"));
//    for (String title : Arrays.asList("Family Physician", "General Internal Medicine")) {
//      panel.add(ratingBox("type", title, null));
//      panel.add(new InlineHTML("<br/>"));
//    }
    panel.add(h2(new HTML(
        "<b>SPECIALISTS</b>: These doctors often require a physician's recommendation to be seen.")));
    for (String title : Arrays.asList("Allergist", "Cardiologist", "Dentist", "Endocrinologist",
        "Gastroenterologist", "Gynecologist", "Immunologist", "Infectious Disease Specialist", "Neurologist",
        "Orthopedist", "Ear, Nose and Throat Physician", "Pain Management Specialist", "Pediatrician",
        "Physical Therapist and Rehabilitation Specialist",
        "Psychological Counselor (psychiatrist, psychologist, social worker, etc.)", "Rheumatologist",
        "Sleep Specialist", "Sports Medicine Doctor", "Not Sure")) {
      panel.add(ratingBox("type", title, null));
      panel.add(new InlineHTML("<br/>"));
    }

    panel.add(new InlineHTML("Other "));
    panel.add(textInput("typeSpecialistOther"));

    panel.add(h2(new HTML("<b>ALTERNATIVE HEALTH PRACTITIONERS</b>")));
    for (String title : Arrays.asList("MD with an Alternative Focus", "Acupressurist", "Acupuncturist",
        "Alexander Technique Practitioner", "Amygdala Retrainer", "Aromatherapist", "Aryuveda practitioner",
        "Bach Flower", "Chinese Medicine Specialist", "Chiropracter", "Colon/Hydrotherapist",
        "Cranio-sacral Therapist", "Emotional Freedom Technique (EFT) Practitioner",
        "Energy Healing Practitioner", "Environmental Medicine", "Feldenkrais Practitioner", "Herbalist",
        "Homeopathist", "Hypnotherapist", "Kinesiologist", "Lightning Process Practitioner",
        "Macrobiotic Practitioner", "Massage Therapist",
        "Mindfulness Based Stress Reduction (MBSR) Practitioner", "Naturopathist",
        "Nambudripad Allergy Technique (NAET) Practitioner", "Neuro-linguistic Programmer", "Nutritionist",
        "Orthomolecular Medicine", "Osteopath", "Qigong Practitioner", "Reiki Practitioner", "Reflexologist",
        "Spiritual Healer", "Rolfer")) {
      panel.add(ratingBox("type", title, null));
      panel.add(new InlineHTML("<br/>"));
    }

    panel.add(new InlineHTML("Other "));
    panel.add(textInput("typeAlternativeOther"));
    panel.add(new InlineHTML("<br/>"));

    panel.add(wizardNavigation(null));
  }

  /** First doctor evaluation page */
  protected void secondStep() {
    panel.add(h3(new InlineHTML("EXPERIENCE:")));
    panel.add(new InlineHTML("Please provide your assessment"
        + " of your health professional's experience level at treating this disease."));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("experience", null, "<br/>",//
        "Specialist", "<b>Specialist</b> - This person specializes in treating ME/CFS;"
            + " most of her/his patients have chronic fatigue syndrome.",//
        "Knowledgeable", "<b>Knowledgeable</b> - The 'Knowledgeable' may not specialize"
            + " in chronic fatigue syndrome (ME/CFS)"
            + " but these patients make up a significant portion of her/his practice.",//
        "Informed", "<b>Informed</b> - Chronic fatigue syndrome (ME/CFS)"
            + " is not a major part of this person's practice"
            + " but they appear to be knowledgeable about the disease and its treatment options.",//
        "Learner", "<b>Learner</b> - The 'Learner' does not treat"
            + " many chronic fatigue syndrome (ME/CFS) patients"
            + " but is willing to learn and listen to and review patient suggestions.",//
        "Uninformed", "<b>Uninformed</b> - The 'Uninformed' practitioner"
            + " doesn't know much about the disease and is not interested.",//
        "Skeptic", "<b>Skeptic</b> - The 'Skeptic' practitioner" + " does not believe ME/CFS exists"
            + " and appears to take its existence as a personal affront.",//
        "-", "<b>I don't know</b>"));

    panel.add(h3(new InlineHTML("INITIAL COST:")));
    panel.add(new InlineHTML(
        "Since costs can vary greatly even for patients seeing the same health professional"
            + " - depending on the treatment regimen -"
            + " we'd like to know the approximate initial costs of seeing your health professional."
            + " This includes the approximate fees for the first two visits;"
            + " the costs of the laboratory workup and whatever medications were prescribed."));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("initialCost", null, "<br/>", "<$100", "Less than $100", "$100-$500", "$100-$500",
        "$500-$1000", "$500-$1,000", "$1000-$2000", "$1.000-$2.000", "$2000-$5000", "$2,000-$5,000",
        ">$5000", "&gt;$5,000", "NotSure", "Not Sure"));

    panel.add(h3(new InlineHTML("COST:")));
    panel
        .add(new InlineHTML("Average 6 months cost"
            + " - please include doctor's visits, tests and treatment program."
            + " Please do not include travel"));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("averageCost", null, "<br/>", //
        "<600", "Less than $600 (>$100/month)", //
        "600-1200", "$600-$1200 ($100-$200/month)", //
        "1200-2400", "$1200-$2400 ($200-400/month)", //
        "2400-4800", "$2400-$4800 ($400-$800/month)", //
        ">4800", ">$4800 (>$800/month)"));

    panel.add(h3(new InlineHTML("ACCEPTS INSURANCE?")));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("insurance", "-", "<br/>", "Yes", "Yes", "No", "No", "-", "Don't Know"));

    panel.add(h3(new InlineHTML("AVERAGE LENGTH OF VISIT (not including the first two sessions):")));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("visitLength", null, "<br/>", "<15m", "Less than 15 minutes", "15m-30m",
        "15 to 30 minutes", "30m-1h", "30 minutes to an hour", ">1h", "Greater than an hour"));

    panel.add(h3(new InlineHTML("TREATMENT BREADTH -")));
    panel.add(new InlineHTML("This practitioner provides <b>significant</b> information on:"));
    panel.add(new InlineHTML("<br/>"));
    panel.add(ratingBox("treatmentBreadth", "drugs", "Pharmaceutical Drugs"));
    panel.add(new InlineHTML("<br/>"));
    panel.add(ratingBox("treatmentBreadth", "alternativeTreatments",
        "Alternative Treatments (vitamins, neutraceuticals, etc.)"));
    panel.add(new InlineHTML("<br/>"));
    panel.add(ratingBox("treatmentBreadth", "lifestyle",
        "Lifestyle Management (envelope therapy, pacing, sleep hygiene, behavioral therapies, etc.)"));
    panel.add(new InlineHTML("<br/>"));

    panel.add(h3(new InlineHTML("MEDICATION PURCHASING")));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("ripoff", "No", "<br/>", "Yes",
        "Practitioner requires that patients buy alternative medications/neutraceuticals from her/him", "No",
        "Practitioner allows patients to buy alternative medications/neutraceuticals from outside sources"));

    panel.add(h3(new InlineHTML("OFFICE MANAGEMENT AND ORGANIZATION")));
    panel.add(new HTML("Please rate on scale from 1-5 how well organized this practitioners office was"
        + " (1 = chaotic, 5 = humming like a well-oiled machine)."
        + " This applies to such things as scheduling, receiving test results on time,"
        + " getting documents to and from the practitioner, etc."));
    panel.add(radioNumeric("organization", 1, 5));

    panel.add(h3(new InlineHTML("AVAILABILITY")));
    panel
        .add(new HTML(
            "Please rate on a scale from 1-5 how available was the practitioner to you"
                + " (1= not available outside of office visits, 5= quick response)?"
                + " Did they respond in a timely manner to request and questions or did you have to wait, wait, wait....?"));
    panel.add(radioNumeric("availability", 1, 5));

    final Command verifier = new Command() {
      @Override public void execute() {
        if (rating.getField("experience") == null) {
          final HTML html = new HTML(
              "<span style='color: red; font-size: larger'>Please assess the experience level! Thanks.</span>");
          panel.add(html);
          Document.get().setScrollTop(
              html.getAbsoluteTop() + html.getOffsetHeight() - Window.getClientHeight());
          throw new CommandCanceledException(this);
        }
      }
    };
    panel.add(wizardNavigation(verifier));
  }

  protected void thirdStep() {
    panel.add(new HTML("PATIENT INFORMATION"));

    panel.add(h3(new InlineHTML("AGE")));
    panel.add(radioInput("age", null, "<br/>", "<12", "&lt; 12 years old", "12-18", "12-18 years old",
        "19-30", "19-30 years old", "31-40", "31-40 years old", "41-50", "41-50 years old", "51-60",
        "51-60 years old", "61-70", "61-70 years old", ">70", "&gt; 70 years old"));

    panel.add(h3(new InlineHTML("GENDER")));
    panel.add(radioInput("gender", null, "<br/>", "f", "Female", "m", "Male"));

    panel.add(h3(new InlineHTML("REASON: ")));
    panel.add(new InlineHTML("What is the main reason you choose to see this practitioner?"));
    panel.add(radioInput("reason", null, "<br/>", //
        "primary", "He/she is my primary care physician", //
        "hpnet", "He/she was in my health provider network", //
        "referred", "I was referred to him/her by another practitioner", //
        "my own research", "I decided based on my own research to see him/her", //
        "heard", "I heard about him/her from the internet/another person with CFS"));

    panel.add(h3(new InlineHTML("DISTANCE: ")));
    panel.add(new InlineHTML("How far did you travel to see this practitioner?"));
    panel.add(radioInput("distance", null, "<br/>", //
        ">50", ">50 miles", //
        "50-100", "50-100 miles", //
        "100-500", "100-500 miles", //
        "500-1000", "500-1,000 miles", //
        ">1000", ">1,000 miles"));

    panel.add(h3(new InlineHTML("LENGTH OF TIME SEEING PRACTITIONER")));
    panel.add(radioInput("seeingTime", null, "<br/>", "<6m", "Less than 6-months", "6m-1y",
        "Six Months to a Year", "1-2y", "One to Two Years", "2-5y", "Two to Five Years", ">5y",
        "Greater than Five Years"));

    panel.add(h3(new InlineHTML("YEARS WITH ME/CFS (CHRONIC FATIGUE SYNDROME)")));
    panel.add(radioInput("beingIll", null, "<br/>", "<1y", "Less Than One Year", "1-2y", "One to Two Years",
        "2-5y", "Two to Five Years", "5-10y", "Five to Ten Years", "10-20y", "Ten to Twenty Years", ">20y",
        "Greater than Twenty Years"));

    panel.add(wizardNavigation(null));
  }

  /** Second doctor evaluation page (?) */
  protected void fourthStep() {
    final FlexTable table = new FlexTable();
    final FlexTable.FlexCellFormatter formatter = table.getFlexCellFormatter();
    table.addStyleName("GrondFourthStepTable");

    panel.add(h3(new InlineHTML("ACTIVITY LEVEL WHEN YOU FIRST STARTED SEEING THIS PRACTITIONER")));
    final String[] activityLevels = {
        "1",
        "<b>One</b> - Bed-ridden, up to bathroom only",
        "2",
        "<b>Two</b> - Out of bed 30 min-2 hrs/day",
        "3",
        "<b>Three</b> - Out of bed 2-4 hrs/day",
        "4",
        "<b>Four</b> - Out of bed 4-6 hrs/day",
        "5",
        "<b>Five</b> - Minimal bed time needed, can work sedentary part-time job",
        "6",
        "<b>Six</b> - May maintain 40 hr sedentary work week"
            + " with limited housework/social activities, daily naps needed", "7",
        "<b>Seven</b> - Able to work a sedentary job" + " plus light housekeeping &ndash; no naps needed",
        "8", "<b>Eight</b> - Able to manage full work (sedentary)" + " plus manage a household", "9",
        "<b>Nine</b> - May exercise at approximately" + " ½-2/3rds normal without excessive fatigue", "10",
        "<b>Ten</b> - Normal" };
    panel.add(radioInput("actLevStart", null, "<br/>", activityLevels));

    panel.add(h3(new InlineHTML("ACTIVITY LEVEL WHEN YOU LAST SAW THIS PRACTITIONER")));
    panel.add(radioInput("actLevEnd", null, "<br/>", activityLevels));

    table.setHTML(0, 1, "Please rate your symptom level"
        + " the first time you saw your practitioner and the last time you saw her/him");
    formatter.setColSpan(0, 1, 22);
    table.setHTML(1, 1, "AT FIRST APPOINTMENT");
    formatter.setColSpan(1, 1, 10);
    formatter.setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_CENTER);
    table.setHTML(1, 3, "AT LAST APPOINTMENT");
    formatter.setColSpan(1, 3, 11);
    formatter.setHorizontalAlignment(1, 3, HasHorizontalAlignment.ALIGN_CENTER);

    table.setHTML(3, 1, "Worst");
    formatter.setColSpan(3, 1, 5);
    table.setHTML(3, 2, "Healthy");
    formatter.setColSpan(3, 2, 5);
    formatter.setHorizontalAlignment(3, 2, HasHorizontalAlignment.ALIGN_RIGHT);

    formatter.setWidth(3, 3, "4px");

    table.setHTML(3, 4, "Worst");
    formatter.setColSpan(3, 4, 5);
    table.setHTML(3, 5, "Healthy");
    formatter.setColSpan(3, 5, 5);
    formatter.setHorizontalAlignment(3, 5, HasHorizontalAlignment.ALIGN_RIGHT);

    table.setHTML(2, 22, "N/A");
    formatter.setHorizontalAlignment(2, 2, HasHorizontalAlignment.ALIGN_CENTER);

    /*! Maps cell index to a button. */
    final HashMap<String, RadioButton> buttons = new HashMap<String, RadioButton>();
    final int cellWidth = 24; /*!< Radio button cells should have an equal width. */
    final String cellWidthS = cellWidth + "px";
    final ValueChangeHandler<Boolean> buttonChangeHandler = new ValueChangeHandler<Boolean>() {
      @Override public void onValueChange(ValueChangeEvent<Boolean> event) {
        final RadioButton button = (RadioButton) event.getSource();
        ratingUpdateString(button.getName(), button.getFormValue());
      }
    };
    for (int i = 1; i <= 10; ++i) {
      table.setHTML(2, i, Integer.toString(i));
      table.setHTML(2, 11 + i, Integer.toString(i));
      formatter.setHorizontalAlignment(2, i, HasHorizontalAlignment.ALIGN_CENTER);
      formatter.setHorizontalAlignment(2, 11 + i, HasHorizontalAlignment.ALIGN_CENTER);
      formatter.setWidth(2, i, cellWidthS);
      formatter.setWidth(2, 11 + i, cellWidthS);
      int row = 4;
      for (final String field : DoctorRating.levelPrefixes()) {
        if (i == 1) {
          table.setHTML(row, 0, DoctorRating.levelLabel(field));

          formatter.setWidth(2, 22, cellWidthS);

          final RadioButton naButton = new RadioButton(field + "After");
          naButton.setFormValue("-1");
          naButton.setValue(naButton.getFormValue().equals(rating.getField(naButton.getName())));
          table.setWidget(row, 22, naButton);
          naButton.addValueChangeHandler(buttonChangeHandler);
          buttons.put(row + ",22", naButton);
          formatter.addStyleName(row, 22, "GrondRadioRow");
        }
        if (!field.equals("sat")) {
          final RadioButton button = new RadioButton(field + "Before");
          button.setFormValue(Integer.toString(i));
          button.setValue(button.getFormValue().equals(rating.getField(button.getName())));
          table.setWidget(row, i, button);
          button.addValueChangeHandler(buttonChangeHandler);
          buttons.put(row + "," + i, button);
          formatter.addStyleName(row, i, "GrondRadioRow");
        }
        final RadioButton button = new RadioButton(field + "After");
        button.setFormValue(Integer.toString(i));
        button.setValue(button.getFormValue().equals(rating.getField(button.getName())));
        table.setWidget(row, 11 + i, button);
        button.addValueChangeHandler(buttonChangeHandler);
        buttons.put(row + "," + (11 + i), button);
        formatter.addStyleName(row, 11 + i, "GrondRadioRow");

        ++row;
      }
    }
    // Handle cell clicks as radio button clicks.
    table.addClickHandler(new ClickHandler() {
      @Override public void onClick(final ClickEvent event) {
        final HTMLTable.Cell cell = table.getCellForEvent(event);
        if (cell != null) {
          final RadioButton button = buttons.get(cell.getRowIndex() + "," + cell.getCellIndex());
          if (button != null && !button.getValue()) button.setValue(true);
        }
      }
    });
    panel.add(table);

    panel.add(h2(new HTML("Comments")));
    final TextArea comments = new TextArea();
    comments.setValue(rating.getField("patientComments") != null ? (String) rating
        .getField("patientComments") : "");
    comments.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override public void onValueChange(ValueChangeEvent<String> event) {
        ratingUpdateString("patientComments", event.getValue());
      }
    });
    comments.addStyleName("GrondRatingTextarea");
    panel.add(comments);

    panel.add(h2(new HTML("Name:")));
    panel.add(textInput("patientName"));
    panel.add(new HTML("You are not required to leave your name but we encourage you to do so."
        + " If you do it will appear on your review." + " Reviews from patients who leave their names"
        + " will receive a higher rating than anonymous reviews."));

    panel.add(h2(new HTML("E-mail Address:")));
    final TextBox patientEmail = textInput("patientEmail");
    if (patientEmail.getValue().length() == 0 && grond.currentUser != null && grond.currentUser.email != null
        && grond.currentUser.email.length() != 0) patientEmail.setValue(grond.currentUser.email, true);
    panel.add(patientEmail);
    panel.add(new HTML("If you wish to edit this review in the future"
        + " or to do further reviews please provide your e-mail address;"
        + " it will not be visible on the internet."));

    panel.add(new HTML("&nbsp;"));
    panel.add(new HTML("Thanks for taking the time to fill out the form!"));

    final Command verifier = new Command() {
      @Override public void execute() {
        if (rating.satAfter <= 0) {
          final HTML html = new HTML(
              "<span style='color: red; font-size: larger'>Please provide the overall satisfaction!</span>");
          panel.add(html);
          Document.get().setScrollTop(
              html.getAbsoluteTop() + html.getOffsetHeight() - Window.getClientHeight());
          throw new CommandCanceledException(this);
        }
      }
    };
    panel.add(wizardNavigation(verifier));
  }

  protected void finish() {
    History.newItem("mapOf_" + countryId + "_" + condition, true);
  }

  protected Panel wizardNavigation(Command nextVerifier) {
    final FlowPanel panel = new FlowPanel();
    if (step > 1) panel.add(stepButton("<- back", step - 1, null));
    if (step < 4) panel.add(stepButton("next ->", step + 1, nextVerifier));
    else if (step == 4) panel.add(stepButton("finish", step + 1, nextVerifier));
    return panel;
  }

  protected Panel radioInput(final String field, final String defaultValue, final String sep,
      final String... valuesAndLabels) {
    final String haveValue = (String) rating.getField(field);

    final FlowPanel panel = new FlowPanel();
    for (int i = 0; i < valuesAndLabels.length; i += 2) {
      final String value = valuesAndLabels[i];
      final String label = valuesAndLabels[i + 1];
      final RadioButton radio = new RadioButton(field, label, true);
      radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        @Override public void onValueChange(ValueChangeEvent<Boolean> event) {
          ratingUpdateString(field, value);
        }
      });
      if (haveValue != null) {
        if (haveValue.equals(value)) radio.setValue(true, false); // Check.
      } else if (defaultValue != null && defaultValue.equals(value)) radio.setValue(true, true); // Check and save.
      panel.add(radio);
      panel.add(new InlineHTML(sep));
    }
    return panel;
  }

  protected Panel radioNumeric(final String field, final int from, final int till) {
    final LinkedList<String> list = new LinkedList<String>();
    for (int num = from; num <= till; ++num) {
      final String str = Integer.toString(num);
      list.add(str);
      list.add(str);
    }
    return radioInput(field, null, " ", (String[]) list.toArray(new String[list.size()]));
  }

  protected Widget h2(Widget widget) {
    widget.addStyleName("GrondRatingH2");
    return widget;
  }

  protected Widget h3(Widget widget) {
    widget.addStyleName("GrondRatingH3");
    return widget;
  }

  protected Button stepButton(final String label, final int targetStep, final Command verifier) {
    final Button button = new Button(label);
    button.addClickHandler(new ClickHandler() {
      @Override public void onClick(ClickEvent event) {
        if (verifier != null) try {
          verifier.execute();
        } catch (CommandCanceledException cancel) {
          return; // Verifier blocked the button.
        }

        panel.clear();
        step = targetStep;
        addToPanel();
        Document.get().setScrollTop(panel.getAbsoluteTop());
      }
    });
    return button;
  }

  protected void ratingUpdateString(final String field, String value) {
    if (value == null) value = "";
    rating.setField(field, value);
    if (value.length() == 0) grond.getGae().ratingRemove(ratingId, field);
    else grond.getGae().ratingUpdateString(ratingId, field, value);

    // Force loading fresh rated doctor's list if the rating was changed. 
    grond.getGae().cleanCache("^getDoctorsByRating");
  }

  protected TextBox textInput(final String field) {
    final String haveValue = (String) rating.getField(field);
    final TextBox textBox = new TextBox();
    textBox.setValue(haveValue != null ? haveValue : "");
    textBox.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override public void onValueChange(ValueChangeEvent<String> event) {
        ratingUpdateString(field, event.getValue());
      }
    });
    return textBox;
  }

  /** Update local AND remote copies of the rating. */
  protected void ratingUpdateList(final String field, final String value, final boolean add) {
    // Update the local copy of the rating.
    @SuppressWarnings("unchecked")
    ArrayList<String> values = (ArrayList<String>) rating.getField(field);
    if (values == null) values = new ArrayList<String>();
    boolean valueAlreadySet = false;
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) != null && value.equals(values.get(i))) {
        if (add) valueAlreadySet = true;
        else values.remove(i--);
      }
    }
    if (!valueAlreadySet && add) values.add(value);
    rating.setField(field, values);

    // Update the server copy.
    grond.getGae().ratingUpdateList(ratingId, field, value, add);
  }

  /** The value of this box is automatically synchronized with the rating and saved to the server. */
  protected CheckBox ratingBox(final String field, final String value, String label) {
    if (label == null) label = value;
    final CheckBox box = new CheckBox(label);
    // See if the box is checked in the rating.
    assert (rating != null);
    if (rating.getField(field) != null) {
      @SuppressWarnings("unchecked")
      final ArrayList<String> values = (ArrayList<String>) rating.getField(field);
      for (int i = 0; i < values.size(); ++i) {
        if (values.get(0) != null && value.equals(values.get(i))) {
          box.setValue(true);
          break;
        }
      }
    }
    box.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override public void onValueChange(final ValueChangeEvent<Boolean> event) {
        ratingUpdateList(field, value, event.getValue());
      }
    });
    return box;
  }
}
