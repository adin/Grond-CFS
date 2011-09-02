package grond.client;

import grond.shared.Fields;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
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

    try {
      final JSONValue treatmentBreadthPercentSpread = trpInfo.get("treatmentBreadthPercentSpread");
      if (treatmentBreadthPercentSpread != null) {
        final JSONArray spread = treatmentBreadthPercentSpread.isArray();
        if (spread.size() > 0) panel.add(new HTML(
            "This practitioner provides <b>significant</b> information on:"));
        for (int num = 0; num < spread.size(); num += 2) {
          final String value = spread.get(num).isString().stringValue();
          final int percent = (int) spread.get(num + 1).isNumber().doubleValue();
          String desc = value;
          if (desc.equals("drugs")) desc = "Pharmaceutical Drugs";
          if (desc.equals("alternativeTreatments")) desc = "Alternative Treatments (vitamins, neutraceuticals, etc.)";
          if (desc.equals("lifestyle")) desc = "Lifestyle Management (envelope therapy, pacing, sleep hygiene, behavioral therapies, etc.)";
          panel.add(new HTML("<span style=\"padding-left: 1em\">" + percent + "% said yes for " + desc
              + "<span>"));
        }
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final JSONValue alsa = trpInfo.get("actLevStart_average");
      if (alsa != null) {
        int av = (int) alsa.isNumber().doubleValue();
        panel.add(new Label(
            "Level; average activity (level) of patients when they first saw this practitioner - " + av));
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final JSONValue apc = trpInfo.get("averagePatientCondition");
      final JSONValue allPatients = trpInfo.get("averageAllPatientsCondition");
      if (apc != null && apc.isObject() != null) {
        final JSONObject apco = apc.isObject();
        boolean haveValues = false;
        for (final String field : Fields.levelPrefixes()) {
          final JSONValue value = apco.get(field + "Before");
          if (value != null && value.isNumber() != null) {
            haveValues = true;
            break;
          }
        }
        if (haveValues) {
          panel.add(new Label("Average Patient Condition at the beginning of treatment for this doctor"
              + " / average for all doctors"));
          final FlexTable table = new FlexTable();
          int row = 0;
          for (final String field : Fields.levelPrefixes()) {
            if (!field.equals("sat")) { // There isn't `satBefore`.
              table.setHTML(row, 0, "&nbsp;&nbsp;&nbsp;");
              table.setHTML(row, 1, Fields.levelLabel(field));
              table.setHTML(row, 2, "&nbsp;&nbsp;&nbsp;");
              final JSONValue value = apco.get(field + "Before");
              if (value != null && value.isNumber() != null) {
                table.setHTML(row, 3, Integer.toString((int) value.isNumber().doubleValue()));
              } else {
                table.setHTML(row, 3, "-");
              }
              if (allPatients != null && allPatients.isObject() != null) {
                final JSONValue allValue = allPatients.isObject().get(field + "Before");
                if (allValue != null && allValue.isNumber() != null) {
                  table.setHTML(row, 4, "&nbsp;&nbsp;&nbsp;");
                  table.setHTML(row, 5, Integer.toString((int) allValue.isNumber().doubleValue()));
                }
              }
              ++row;
            }
          }
          panel.add(table);
        }
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final JSONValue apg = trpInfo.get("averagePatientGain");
      if (apg != null && apg.isObject() != null) {
        panel.add(new Label("Average GAIN"));
        final FlexTable table = new FlexTable();
        final JSONObject apgo = apg.isObject();
        int row = 0;
        for (final String field : Fields.levelPrefixes()) {
          if (!field.equals("sat")) { // There isn't `satBefore`.
            table.setHTML(row, 0, "&nbsp;&nbsp;&nbsp;");
            table.setHTML(row, 1, Fields.levelLabel(field));
            table.setHTML(row, 2, "&nbsp;&nbsp;&nbsp;");
            final JSONValue value = apgo.get(field);
            if (value != null && value.isNumber() != null) {
              final StringBuilder sb = new StringBuilder();
              final int gain = (int) value.isNumber().doubleValue();
              if (gain <= 0) {
                sb.append("<span class=\"NegativeGain\">");
                for (int i = 0; i < -gain; ++i)
                  sb.append('▄');
                sb.append("</span>");
              } else {
                sb.append("<span class=\"PositiveGain\">");
                for (int i = 0; i < gain; ++i)
                  sb.append('█');
                sb.append("</span>");
              }
              table.setHTML(row, gain > 0 ? 4 : 3, sb.toString());
              table.getCellFormatter().setAlignment(row, 3, HasHorizontalAlignment.ALIGN_RIGHT,
                  HasVerticalAlignment.ALIGN_MIDDLE);
            }
            ++row;
          }
        }
        panel.add(table);
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

/*
Last Table : 

Satisfaction Rating

Ave for this practitioners/ Average for all practitioners 
 */
  }
}
