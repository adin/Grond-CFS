package grond.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.annotation.Unindexed;

@Cached(expirationSeconds = 3600) public class DoctorRating implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id public Long id;
  // NB: To use `Key` in GWT classes we need Objectify GWT module inherited.
  @Parent public Key<Doctor> doctor;

  public String country;
  public String region;
  public String city;
  public String firstName;
  public String lastName;

  public String webSite;

  /** User.getFederatedIdentity or User.getUserId */
  public String user;
  public String userEmail;
  /** "fm" or "cfs" */
  @Unindexed public String condition;

  public Date lastUpdate;

  // Indexed form fields.
  /** Indexed copy of `levels("satAfter")` */
  public int satAfter;

  // Unindexed form fields.
  @Unindexed public ArrayList<String> type;
  @Unindexed public String typeSpecialistOther;
  @Unindexed public String typeAlternativeOther;
  @Unindexed public String experience;
  @Unindexed public String initialCost;
  @Unindexed public String averageCost;
  @Unindexed public String insurance;
  @Unindexed public String visitLength;
  @Unindexed public String visitFrequency;
  @Unindexed public ArrayList<String> treatmentBreadth;
  @Unindexed public String ripoff;
  @Unindexed public String organization;
  @Unindexed public String availability;
  @Unindexed public String age;
  /** "f" or "m" */
  @Unindexed public String gender;
  @Unindexed public String reason;
  @Unindexed public String distance;
  @Unindexed public String seeingTime;
  @Unindexed public String beingIll;
  @Unindexed public int actLevStart;
  @Unindexed public int actLevEnd;
  @Unindexed @Serialized public HashMap<String, Integer> levels;
  @Unindexed public String patientComments;
  @Unindexed public String patientName;
  @Unindexed public String patientEmail;

  // Fields passed to GWT (see ServerIf#nameAndLocation, ServerIf#getRating).
  /** This field is only available in GWT, do not use it in GAE. */
  @Transient public Doctor _doctor;
  /** This field is only available in GWT, do not use it in GAE. */
  @Transient public boolean _doctorCreated;

  public static List<String> experienceLevels() {
    return Arrays.asList("Skeptic", "Uninformed", "Learner", "Informed", "Knowledgeable", "Specialist");
  }

  /** Symtom levels of the first and last visits (the corresponding fields are suffixed with "Before" and "After"). */
  public static List<String> levelPrefixes() {
    return Arrays.asList("energy", "sleep", "think", "pain", "mood", "ql", "sat");
  }

  public static String levelLabel(final String levelPrefix) {
    if (levelPrefix.equals("energy")) return "Ability to exercise";
    return levelPrefix.equals("think") ? "Thinking Ability" : levelPrefix.equals("ql") ? "Quality of Life"
        : levelPrefix.equals("sat") ? "Overall Satisfaction" : levelPrefix.substring(0, 1).toUpperCase()
            + levelPrefix.substring(1);
  }

  @Override public int hashCode() {
    return ((int) doctor.getId()) + id.intValue();
  }

  @Override public boolean equals(Object obj) {
    if (!(obj instanceof DoctorRating)) return false;
    final DoctorRating dr = (DoctorRating) obj;
    return id.longValue() == dr.id.longValue() && doctor.getId() == dr.doctor.getId();
  }

  public boolean isFinished() {
    return satAfter > 0;
  }

  /** Includes the doctor id necessary to positively identify the rating. */
  public String getFullId() {
    return doctor.getId() + "." + id;
  }

  /** Reflective update of fields, currently used by RatingForm and GaeImpl. */
  @SuppressWarnings("unchecked") public void setField(final String field, final Object value) {
    if (field.equals("condition")) condition = (String) value;
    else if (field.equals("type")) type = (ArrayList<String>) value;
    else if (field.equals("typeSpecialistOther")) typeSpecialistOther = (String) value;
    else if (field.equals("typeAlternativeOther")) typeAlternativeOther = (String) value;
    else if (field.equals("experience")) experience = (String) value;
    else if (field.equals("initialCost")) initialCost = (String) value;
    else if (field.equals("averageCost")) averageCost = (String) value;
    else if (field.equals("insurance")) insurance = (String) value;
    else if (field.equals("visitLength")) visitLength = (String) value;
    else if (field.equals("visitFrequency")) visitFrequency = (String) value;
    else if (field.equals("treatmentBreadth")) treatmentBreadth = (ArrayList<String>) value;
    else if (field.equals("ripoff")) ripoff = (String) value;
    else if (field.equals("organization")) organization = (String) value;
    else if (field.equals("availability")) availability = (String) value;
    else if (field.equals("age")) age = (String) value;
    else if (field.equals("gender")) gender = (String) value;
    else if (field.equals("reason")) reason = (String) value;
    else if (field.equals("distance")) distance = (String) value;
    else if (field.equals("seeingTime")) seeingTime = (String) value;
    else if (field.equals("beingIll")) beingIll = (String) value;
    else if (field.equals("actLevStart")) actLevStart = Integer.parseInt(value.toString());
    else if (field.equals("actLevEnd")) actLevEnd = Integer.parseInt(value.toString());
    else if (field.equals("patientComments")) patientComments = (String) value;
    else if (field.equals("patientName")) patientName = (String) value;
    else if (field.equals("patientEmail")) patientEmail = (String) value;
    else if (field.equals("webSite")) webSite = (String) value;
    else {
      int levelValue = -1;
      Logger.getLogger("setField").info("field: " + field);
      for (final String levelPrefix : levelPrefixes())
        if (field.equals(levelPrefix + "Before") || field.equals(levelPrefix + "After")) {
          if (levels == null) levels = new HashMap<String, Integer>();
          levelValue = Integer.parseInt(value.toString());
          levels.put(field, levelValue);
          if (field.equals("satAfter")) satAfter = levelValue;
          break;
        }
      Logger.getLogger("setField").info(
          "field: " + field + "; levelValue: " + levelValue + "; levels: " + levels);
      if (levelValue == -1) throw new RuntimeException("DoctorRating.setField: unknown field: " + field);
    }
  }

  /** Reflective retrieval of fields, currently used by RatingForm. */
  public Object getField(final String field) {
    if (field == null || field.length() == 0) return null;
    else if (field.equals("condition")) return condition;
    else if (field.equals("type")) return type;
    else if (field.equals("typeSpecialistOther")) return typeSpecialistOther;
    else if (field.equals("typeAlternativeOther")) return typeAlternativeOther;
    else if (field.equals("experience")) return experience;
    else if (field.equals("initialCost")) return initialCost;
    else if (field.equals("averageCost")) return averageCost;
    else if (field.equals("insurance")) return insurance;
    else if (field.equals("visitLength")) return visitLength;
    else if (field.equals("visitFrequency")) return visitFrequency;
    else if (field.equals("treatmentBreadth")) return treatmentBreadth;
    else if (field.equals("ripoff")) return ripoff;
    else if (field.equals("organization")) return organization;
    else if (field.equals("availability")) return availability;
    else if (field.equals("age")) return age;
    else if (field.equals("gender")) return gender;
    else if (field.equals("reason")) return reason;
    else if (field.equals("distance")) return distance;
    else if (field.equals("seeingTime")) return seeingTime;
    else if (field.equals("beingIll")) return beingIll;
    else if (field.equals("actLevStart")) return Integer.toString(actLevStart);
    else if (field.equals("actLevEnd")) return Integer.toString(actLevEnd);
    else if (field.equals("patientComments")) return patientComments;
    else if (field.equals("patientName")) return patientName;
    else if (field.equals("patientEmail")) return patientEmail;
    else if (field.equals("webSite")) return webSite;
    else for (final String levelPrefix : levelPrefixes())
      if (field.equals(levelPrefix + "Before") || field.equals(levelPrefix + "After")) return levels != null
          && levels.containsKey(field) ? levels.get(field).toString() : "";
    throw new RuntimeException("DoctorRating.getField: unknown field: " + field);
  }
}
