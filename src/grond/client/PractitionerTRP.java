package grond.client;

import grond.shared.Doctor;
import grond.shared.DoctorRating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  Doctor doctor;
  HashMap<String, Object> trpInfo;

  /**
   * @param doctor Optional. The Doctor entity, if it is already available.
   */
  PractitionerTRP(Grond grond, final Panel rootPanel, long doctorId, Doctor doctor) {
    this.grond = grond;
    this.panel = new FlowPanel();
    rootPanel.add(this.panel);

    this.doctorId = doctorId;
    this.doctor = doctor;
    final PractitionerTRP self = this;
    grond.getGae().getDoctorTRP(doctorId, true, grond.new Callback<Doctor>() {
      @Override public void onSuccess(Doctor result) {
        self.doctor = result;
        trpInfo = result._trpInfo;

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

      if (doctor.firstName != null && doctor.lastName != null) panel.add(new Label(doctor.firstName + " "
          + doctor.lastName));

      if (doctor.city != null && doctor.region != null) panel.add(new Label("Location - " + doctor.city + "/"
          + doctor.region));

      final StringBuilder sb = new StringBuilder();
      if (doctor.type != null) {
// XXX
//        final JSONArray typea = type.isArray();
//        if (typea != null) for (int n = 0; n < typea.size(); n += 2) {
//          sb.append(typea.get(n).isString().stringValue());
//          if (n + 2 < typea.size()) sb.append("; ");
//        }
//        panel.add(new Label("Type - " + sb.toString()));
      }

      if (doctor._experience != null) panel.add(new Label("Average Rated Experience Level - "
          + doctor._experience));

    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    if (trpInfo == null) return;

    try {
      final double insurancePercent = ((Number) trpInfo.get("insurancePercent")).doubleValue();
      panel.add(new Label("Accepts Insurance - " + Math.round(insurancePercent) + " percent say \"yes\""));
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final double ripoffPercent = ((Number) trpInfo.get("ripoffPercent")).doubleValue();
      panel.add(new Label("Medication Purchasing - requests patient use Drs. own supplements. - "
          + Math.round(ripoffPercent) + " percent say \"yes\""));
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      @SuppressWarnings("unchecked")
      final List<Object> spread = (List<Object>) trpInfo.get("treatmentBreadthPercentSpread");
      if (spread != null) {
        if (spread.size() > 0) panel.add(new HTML(
            "This practitioner provides <b>significant</b> information on:"));
        for (int num = 0; num < spread.size(); num += 2) {
          final String value = (String) spread.get(num);
          final int percent = ((Number) spread.get(num + 1)).intValue();
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
      final Number alsa = (Number) trpInfo.get("actLevStart_average");
      if (alsa != null) {
        int av = alsa.intValue();
        panel.add(new Label(
            "Level; average activity (level) of patients when they first saw this practitioner - " + av));
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      @SuppressWarnings("unchecked")
      final Map<String, Number> apc = (Map<String, Number>) trpInfo.get("averagePatientCondition");
      @SuppressWarnings("unchecked")
      final Map<String, Number> allPatients = (Map<String, Number>) trpInfo
          .get("averageAllPatientsCondition");
      if (apc != null && !apc.isEmpty()) {
        boolean haveValues = false;
        for (final String field : DoctorRating.levelPrefixes()) {
          final Number value = (Number) apc.get(field + "Before");
          if (value != null) {
            haveValues = true;
            break;
          }
        }
        if (haveValues) {
          panel.add(new Label("Average Patient Condition at the beginning of treatment for this doctor"
              + " / average for all doctors"));
          final FlexTable table = new FlexTable();
          int row = 0;
          for (final String field : DoctorRating.levelPrefixes()) {
            if (!field.equals("sat")) { // There isn't `satBefore`.
              table.setHTML(row, 0, "&nbsp;&nbsp;&nbsp;");
              table.setHTML(row, 1, DoctorRating.levelLabel(field));
              table.setHTML(row, 2, "&nbsp;&nbsp;&nbsp;");
              final Number value = (Number) apc.get(field + "Before");
              if (value != null) {
                table.setHTML(row, 3, Integer.toString(value.intValue()));
              } else {
                table.setHTML(row, 3, "-");
              }
              if (allPatients != null && !allPatients.isEmpty()) {
                final Number allValue = (Number) allPatients.get(field + "Before");
                if (allValue != null) {
                  table.setHTML(row, 4, "&nbsp;&nbsp;&nbsp;");
                  table.setHTML(row, 5, Integer.toString(allValue.intValue()));
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
      @SuppressWarnings("unchecked")
      final Map<String, Number> apg = (Map<String, Number>) trpInfo.get("averagePatientGain");
      if (apg != null && !apg.isEmpty()) {
        panel.add(new Label("Average GAIN"));
        final FlexTable table = new FlexTable();
        int row = 0;
        for (final String field : DoctorRating.levelPrefixes()) {
          if (!field.equals("sat")) { // There isn't `satBefore`.
            table.setHTML(row, 0, "&nbsp;&nbsp;&nbsp;");
            table.setHTML(row, 1, DoctorRating.levelLabel(field));
            table.setHTML(row, 2, "&nbsp;&nbsp;&nbsp;");
            final Number value = (Number) apg.get(field);
            if (value != null) {
              final StringBuilder sb = new StringBuilder();
              final int gain = value.intValue();
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

    try {

    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

  }
}
