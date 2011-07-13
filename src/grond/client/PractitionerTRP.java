package grond.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONArray;
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
  JSONObject trpInfo;

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
        trpInfo = result;

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
    } else try {

      final JSONValue firstName = doctor.get("firstName");
      final JSONValue lastName = doctor.get("lastName");
      if (firstName != null && lastName != null) panel.add(new Label(firstName.isString().stringValue() + " "
          + lastName.isString().stringValue()));

      final JSONValue city = doctor.get("city");
      final JSONValue region = doctor.get("region");
      if (city != null && region != null) panel.add(new Label("Location - " + city.isString().stringValue()
          + "/" + region.isString().stringValue()));

      final StringBuilder sb = new StringBuilder();
      final JSONValue type = doctor.get("_type");
      if (type != null) {
        final JSONArray typea = type.isArray();
        if (typea != null) for (int n = 0; n < typea.size(); n += 2) {
          sb.append(typea.get(n).isString().stringValue());
          if (n + 2 < typea.size()) sb.append("; ");
        }
        panel.add(new Label("Type - " + sb.toString()));
      }

      final JSONValue experience = doctor.get("_experience");
      if (experience != null) panel.add(new Label("Average Rated Experience Level - "
          + experience.isString().stringValue()));

    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    if (trpInfo == null) return;

    try {
      final double insurancePercent = trpInfo.get("insurancePercent").isNumber().doubleValue();
      panel.add(new Label("Accepts Insurance - " + Math.round(insurancePercent) + " percent say \"yes\""));
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final double ripoffPercent = trpInfo.get("ripoffPercent").isNumber().doubleValue();
      panel.add(new Label("Medication Purchasing - requests patient use Drs. own supplements. - "
          + Math.round(ripoffPercent) + " percent say \"yes\""));
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

/*
Treatment Breadth - provides information on 

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
