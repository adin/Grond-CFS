package grond.client;

import grond.shared.Doctor;
import grond.shared.DoctorRating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;

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
    Window.setTitle("Practitioner Treatment Review Page");

    if (doctor == null) {
      panel.add(new Image(Grond.ajaxLoaderHorisontal1()));
    } else try {

      final Label name = new Label(doctor.firstName + " " + doctor.lastName);
      name.addStyleName("TrpDoctorName");
      if (doctor.firstName != null && doctor.lastName != null) panel.add(name);

      final Label tlt = new Label("Practitioner Type and Location");
      tlt.addStyleName("TrpSectionHead");
      panel.add(tlt);

      if (doctor.city != null && doctor.region != null) panel.add(new Label("Location - " + doctor.city + "/"
          + doctor.region));

      final StringBuilder sb = new StringBuilder();
      if (doctor.type != null && !doctor.type.isEmpty()) {
        final Iterator<String> keys = doctor.type.keySet().iterator();
        for (int n = 0; n < doctor.type.size(); ++n) {
          sb.append(keys.next());
          if (n < doctor.type.size()) sb.append("; ");
        }
        panel.add(new Label("Type - " + sb.toString()));
      }

      // TODO: Website  - add option for website…

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
      final Integer avf = (Integer) trpInfo.get("averageVisitFrequency");
      if (avf != null) panel.add(new Label(
          "Number of times a person physically sees this practitioner during the year: "
              + (avf.intValue() > 12 ? ">12" : avf)));
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }

    try {
      final int[] distances = (int[]) trpInfo.get("distances");
      if (distances != null) {
        panel.add(new Label("Distance Travelled"));
        for (int pt = 0; pt < distances.length; pt += 2) {
          panel.add(new Label(distances[pt] + "-" + distances[pt + 1]));
        }
        VisualizationUtils.loadVisualizationApi(new Runnable() {
          public void run() {
            final PieOptions options = PieChart.createPieOptions();
            options.setWidth(400);
            options.setHeight(240);
            options.set3D(true);
            options.setTitle("Distance Travelled");

            final DataTable data = DataTable.create();
            data.addColumn(ColumnType.STRING, "Distance");
            data.addColumn(ColumnType.NUMBER, "miles");
            data.addRows(4);
            data.setValue(0, 0, "<100");
            ArrayList<ArrayList<Integer>> fitAndRest = partition(toAL(distances), 1, 99);
            data.setValue(0, 1, percent(distances, fitAndRest.get(0)));
            data.setValue(1, 0, "100-500");
            fitAndRest = partition(fitAndRest.get(1), 101, 499);
            data.setValue(1, 1, percent(distances, fitAndRest.get(0)));
            data.setValue(2, 0, "500-1000");
            fitAndRest = partition(fitAndRest.get(1), 501, 999);
            data.setValue(2, 1, percent(distances, fitAndRest.get(0)));
            data.setValue(3, 0, ">1000");
            fitAndRest = partition(fitAndRest.get(1), 1001, 9000);
            data.setValue(3, 1, percent(distances, fitAndRest.get(0)));

            PieChart pie = new PieChart(data, options);
            panel.add(pie);
          }
        }, PieChart.PACKAGE);
      }
    } catch (Exception ex) {
      Logger.getLogger("PractitionerTRP").log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  /** How many ranges contain the value? In percents. */
  protected int percent(final int[] ranges, final ArrayList<Integer> fit) {
    int count = fit.size() / 2;
    int total = ranges.length / 2;
    return count * 100 / total;
  }

  /** Separate the fitting ranges from the rest.<br>
   * Returns the array of fitting ranges and the array of unfitting ranges. */
  protected ArrayList<ArrayList<Integer>> partition(ArrayList<Integer> ranges, final int from, final int till) {
    final ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>(2);
    final ArrayList<Integer> fit = new ArrayList<Integer>();
    final ArrayList<Integer> rest = new ArrayList<Integer>();
    ret.add(fit);
    ret.add(rest);
    for (int pt = 0; pt < ranges.size(); pt += 2) {
      if (ranges.get(pt).intValue() <= from && from < ranges.get(pt + 1).intValue()) {
        fit.add(ranges.get(pt));
        fit.add(ranges.get(pt + 1));
      } else if (ranges.get(pt).intValue() <= till && till < ranges.get(pt + 1).intValue()) {
        fit.add(ranges.get(pt));
        fit.add(ranges.get(pt + 1));
      } else if (from < ranges.get(pt) && ranges.get(pt) < till) {
        fit.add(ranges.get(pt));
        fit.add(ranges.get(pt + 1));
      } else {
        rest.add(ranges.get(pt));
        rest.add(ranges.get(pt + 1));
      }
    }
    return ret;
  }

  protected ArrayList<Integer> toAL(int[] ranges) {
    final ArrayList<Integer> al = new ArrayList<Integer>(ranges.length);
    for (int range : ranges)
      al.add(range);
    return al;
  }
}
