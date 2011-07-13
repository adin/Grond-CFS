package grond.client;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

/** Practitioner Treatment Review Page.<br>
 * Extended information about the practitioner. */
public class PractitionerTRP {
  final Grond grond;
  Panel panel;
  final long doctorId;
  JSONObject doctor;

  /**
   * @param doctor Optional. The Doctor entity, if it is already available.
   */
  PractitionerTRP(Grond grond, final Panel rootPanel, long doctorId, JSONObject doctor) {
    this.grond = grond;
    this.panel = new FlowPanel();
    rootPanel.add(this.panel);

    this.doctorId = doctorId;
    this.doctor = doctor;
    final PractitionerTRP self = this;
    grond.getGae().getDoctorTRP(doctorId, doctor == null, grond.new Callback<JSONObject>() {
      @Override
      public void onSuccess(JSONObject result) {
        final JSONValue freshDoctorEntity = result.get("doctor");
        if (freshDoctorEntity != null) self.doctor = freshDoctorEntity.isObject();

        final Panel oldPanel = self.panel;
        self.panel = new FlowPanel();
        addToPanel();
        rootPanel.remove(oldPanel);
        rootPanel.add(self.panel);
        oldPanel.clear();
      }
    });
  }

  void addToPanel() {
    panel.add(new HTML("Practitioner Treatment Review Page"));
    if (doctor == null) {
      panel.add(new Image(Grond.ajaxLoaderHorisontal1()));
    } else {
      final String firstName = doctor.get("firstName").isString().stringValue();
      final String lastName = doctor.get("lastName").isString().stringValue();
      panel.add(new Label(firstName + " " + lastName));
      final String city = doctor.get("city").isString().stringValue();
      final String region = doctor.get("region").isString().stringValue();
      panel.add(new Label("Location - " + city + "/" + region));

      panel.add(new Label("Type - ")); // TODO
    }

/*
Average Rated Experience Level - 
Accepts Insurance - X percent say.....yes - just take it off the answer to that question
Treatment Breadth - provides information on 
Medication Purchasing - requests patient use Drs. own supplements...... - x percent say yes

Then a section that shows Average Patient Condition - with a statement some practitioners - particularly ME/CFS specialists, may see a sicker group of patients...

Table - Average Patient Condition at the beginning of treatment for this doctor / average for all doctors

 Activity
Energy
Sleep
Thinking Ability
Pain
Mood 
Quality of Life


Then a table has the average benefits showing
Average GAIN - 

Activity level
Energy
Sleep
Thinking Ability
Pain
Mood 
Quality of Life

Last Table : 

Satisfaction Rating

Ave for this practitioners/ Average for all practitioners 
 */
  }
}
