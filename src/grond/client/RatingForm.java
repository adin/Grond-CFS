package grond.client;

import java.util.Arrays;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
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
  public JSONObject rating;
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

    final Grond.Callback<JSONObject> render = grond.new Callback<JSONObject>() {
      public void onSuccess(final JSONObject $rating) {
        assert (ratingId.equals($rating.get("ratingId").isString().stringValue()));
        rating = $rating;

        // Security check: signed in user should be the same as the rating's owner.
        if (grond.currentUser == null) {
          doctorInfo.setHTML("Please sign in.");
          throw new RuntimeException("Not signed in.");
        }
        final String ratingUser = rating.get("user").isString().stringValue();
        final String gaeUser = grond.currentUser.getId();
        if (gaeUser == null || !gaeUser.equals(ratingUser)) {
          doctorInfo.setHTML("Internal error: This rating belongs to a different user."
              + " Make sure you are signed in properly.");
          throw new RuntimeException("Expected user: " + ratingUser + "; got: " + gaeUser);
        }

        final String name = rating.get("doctor").isObject().get("firstName").isString().stringValue();
        final String surname = rating.get("doctor").isObject().get("lastName").isString().stringValue();
        doctorInfo.setHTML("Rating of: " + name + ' ' + surname + ".");

        if (step == 1) firstStep();
        else if (step == 2) secondStep();
        else if (step == 3) thirdStep();
        else throw new RuntimeException("Unknown step: " + step);
      }
    };

    final ValueChangeHandler<Object> continueWithUser = new ValueChangeHandler<Object>() {
      @Override
      public void onValueChange(ValueChangeEvent<Object> event) {
        // Reuse the rating if it is already fetched.
        if (rating != null) render.onSuccess(rating);
        else grond.getGae().getRating(ratingId, render);
      }
    };

    // We need the current user to work with the ratings. Wait for it if it isn't yet available.
    if (grond.currentUser == null) new Timer() {
      double seconds = 0.0;

      @Override
      public void run() {
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
    panel.add(new HTML("TYPE OF HEALTH PROFESSIONAL"));
    for (String title : Arrays.asList("Family Physician", "General Internal Medicine")) {
      panel.add(ratingBox("type", title, null));
      panel.add(new InlineHTML("<br/>"));
    }
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

    panel.add(stepButton("next ->", 2));
  }

  protected void secondStep() {
    panel.add(h3(new InlineHTML("INITIAL COST:")));
    panel.add(new InlineHTML(
        "Since costs can vary greatly even for patients seeing the same health professional"
            + " - depending on the treatment regimen -"
            + " we'd like to know the approximate initial costs of seeing your health professional."
            + " This includes the approximate fees for the first two visits;"
            + " the costs of the laboratory workup and whatever medications were prescribed."));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("initialCost", null, "<$100", "Less than $100", "$100-$500", "$100-$500",
        "$500-$1000", "$500-$1,000", "$1000-$2000", "$1.000-$2.000", "$2000-$5000", "$2,000-$5,000",
        ">$5000", "&gt;$5,000", "NotSure", "Not Sure"));

    panel.add(h3(new InlineHTML("COST:")));
    panel.add(new InlineHTML("Average Cost of a Follow-up Appointment"));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("averageCost", null, "<$100", "Less than $100", "$100-$500", "$100-$500",
        "$500-$1000", "$500-$1,000", ">$1000", "&gt;$1,000", "NotSure", "Not Sure"));

    panel.add(h3(new InlineHTML("ACCEPTS INSURANCE?")));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("insurance", "-", "Yes", "Yes", "No", "No", "-", "Don't Know"));

    panel.add(h3(new InlineHTML("AVERAGE LENGTH OF VISIT (not including the first two sessions):")));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("visitLength", null, "<15m", "Less than 15 minutes", "15m-30m", "15 to 30 minutes",
        "30m-1h", "30 minutes to an hour", ">1h", "Greater than an hour"));

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
    panel.add(radioInput("ripoff", "No", "Yes",
        "Practitioner requires that patients buy alternative medications/neutraceuticals from her/him", "No",
        "Practitioner allows patients to buy alternative medications/neutraceuticals from outside sources"));

    panel.add(h3(new InlineHTML("EXPERIENCE:")));
    panel.add(new InlineHTML("Please provide your assessment"
        + " of your health professional's experience level at treating this disease."));
    panel.add(new InlineHTML("<br/>"));
    panel.add(radioInput("experience", null, "Specialist",
        "<b>Specialist</b> - This person specializes in treating ME/CFS;"
            + " most of her/his patients have chronic fatigue syndrome.", "Expert",
        "<b>Expert</b> - The 'Expert' may not specialize" + " in chronic fatigue syndrome (ME/CFS)"
            + " but these patients make up a significant portion of her/his practice.", "Informed",
        "<b>Informed</b> - Chronic fatigue syndrome (ME/CFS)"
            + " is not a major part of this person's practice"
            + " but they appear to be knowledgeable about the disease and its treatment options.", "Learner",
        "<b>Learner</b> - The 'Learner' does not treat" + " many chronic fatigue syndrome (ME/CFS) patients"
            + " but is willing to learn and listen to and review patient suggestions.", "No Help",
        "<b>No Help</b> - The 'No Help' practitioner"
            + " doesn't know much about the disease and is not interested.", "Harmful",
        "<b>Harmful</b> - The 'Harmful' practitioner" + " does not believe ME/CFS exists"
            + " and appears to take its existence as a personal affront."));

    panel.add(stepButton("<- back", 1));
    panel.add(stepButton("next ->", 3));
  }

  protected void thirdStep() {
    panel.add(new HTML("PATIENT INFORMATION"));

    panel.add(h3(new InlineHTML("AGE")));
    panel.add(radioInput("age", null, "<12", "&lt; 12 years old", "12-18", "12-18 years old", "19-30",
        "19-30 years old", "31-40", "31-40 years old", "41-50", "41-50 years old", "51-60",
        "51-60 years old", "61-70", "61-70 years old", ">70", "&gt; 70 years old"));

    panel.add(h3(new InlineHTML("GENDER")));
    panel.add(radioInput("gender", null, "f", "Female", "m", "Male"));

    panel.add(h3(new InlineHTML("LENGTH OF TIME SEEING PRACTITIONER")));
    panel.add(radioInput("seeingTime", null, "<6m", "Less than 6-months", "6m-1y", "Six Months to a Year",
        "1-2y", "One to Two Years", "2-5y", "Two to Five Years", ">5y", "Greater than Five Years"));

    panel.add(h3(new InlineHTML("YEARS WITH ME/CFS (CHRONIC FATIGUE SYNDROME)")));
    panel.add(radioInput("beingIll", null, "<1y", "Less Than One Year", "1-2y", "One to Two Years", "2-5y",
        "Two to Five Years", "5-10y", "Five to Ten Years", "10-20y", "Ten to Twenty Years", ">20y",
        "Greater than Twenty Years"));

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
    panel.add(radioInput("actLevStart", null, activityLevels));

    panel.add(h3(new InlineHTML("ACTIVITY LEVEL WHEN YOU LAST SAW THIS PRACTITIONER")));
    panel.add(radioInput("actLevEnd", null, activityLevels));

    panel.add(stepButton("<- back", 2));
  }

  protected Panel radioInput(final String field, final String defaultValue, final String... valuesAndLabels) {
    final String haveValue = rating.containsKey(field) ? rating.get(field).isString().stringValue() : null;

    final FlowPanel panel = new FlowPanel();
    for (int i = 0; i < valuesAndLabels.length; i += 2) {
      final String value = valuesAndLabels[i];
      final String label = valuesAndLabels[i + 1];
      final RadioButton radio = new RadioButton(field, label, true);
      radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          ratingUpdateString(field, value);
        }
      });
      if (haveValue != null) {
        if (haveValue.equals(value)) radio.setValue(true, false); // Check.
      } else if (defaultValue != null && defaultValue.equals(value)) radio.setValue(true, true); // Check and save.
      panel.add(radio);
      panel.add(new InlineHTML("<br/>"));
    }
    return panel;
  }

  protected Widget h2(Widget widget) {
    widget.addStyleName("GrondRatingH2");
    return widget;
  }

  protected Widget h3(Widget widget) {
    widget.addStyleName("GrondRatingH3");
    return widget;
  }

  protected Button stepButton(final String label, final int targetStep) {
    final Button button = new Button(label);
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
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
    rating.put(field, new JSONString(value));
    if (value.length() == 0) grond.getGae().ratingRemove(ratingId, field);
    else grond.getGae().ratingUpdateString(ratingId, field, value);
  }

  protected TextBox textInput(final String field) {
    final JSONValue haveValue = rating.get(field);
    final TextBox textBox = new TextBox();
    textBox.setValue(haveValue != null ? haveValue.isString().stringValue() : "");
    textBox.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        ratingUpdateString("typeSpecialistOther", event.getValue());
      }
    });
    return textBox;
  }

  /** Update local AND remote copies of the rating. */
  protected void ratingUpdateList(final String field, final String value, final boolean add) {
    // Update the local copy of the rating.
    final JSONArray values = rating.containsKey(field) ? rating.get(field).isArray() : new JSONArray();
    boolean valueAlreadySet = false;
    for (int i = 0; i < values.size(); ++i) {
      if (values.get(i) != null && value.equals(values.get(i).isString().stringValue())) {
        if (add) valueAlreadySet = true;
        else values.set(i, null);
      }
    }
    if (!valueAlreadySet && add) values.set(values.size(), new JSONString(value));
    rating.put(field, values);

    // Update the server copy.
    grond.getGae().ratingUpdateList(ratingId, "type", value, add);
  }

  /** The value of this box is automatically synchronized with the rating and saved to the server. */
  protected CheckBox ratingBox(final String field, final String value, String label) {
    if (label == null) label = value;
    final CheckBox box = new CheckBox(label);
    // See if the box is checked in the rating.
    assert (rating != null);
    if (rating.containsKey(field)) {
      final JSONArray values = rating.get(field).isArray();
      for (int i = 0; i < values.size(); ++i) {
        if (values.get(0) != null && value.equals(values.get(i).isString().stringValue())) {
          box.setValue(true);
          break;
        }
      }
    }
    box.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(final ValueChangeEvent<Boolean> event) {
        ratingUpdateList(field, value, event.getValue());
      }
    });
    return box;
  }
}
