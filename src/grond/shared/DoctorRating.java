package grond.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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

  /** User.getFederatedIdentity or User.getUserId */
  public String user;
  public String userEmail;
  /** "fm" or "cfs" */
  @Unindexed public String condition;

  public Date lastUpdate;

  // Indexed form fields.
  /** Indexed copy of `levels("satAfter")` */
  public int satAfter; // XXX: Update `satAfter` when it is updated in `levels`.

  // Unindexed form fields.
  @Unindexed public ArrayList<String> type;
  @Unindexed public String typeSpecialistOther;
  @Unindexed public String typeAlternativeOther;
  @Unindexed public String experience;
  @Unindexed public String averageCost;
  @Unindexed public String insurance;
  @Unindexed public String ripoff;
  @Unindexed public ArrayList<String> treatmentBreadth;
  @Unindexed public int actLevStart;
  @Unindexed public int actLevEnd;
  @Unindexed @Serialized public HashMap<String, Integer> levels;

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
    else throw new RuntimeException("DoctorRating.setField: unknown field: " + field);
  }

  /** Reflective retrieval of fields, currently used by RatingForm. */
  public Object getField(final String field) {
    if (field.equals("condition")) return condition;
    else if (field.equals("type")) return type;
    else if (field.equals("typeSpecialistOther")) return typeSpecialistOther;
    else if (field.equals("typeAlternativeOther")) return typeAlternativeOther;
    else throw new RuntimeException("DoctorRating.getField: unknown field: " + field);
  }
}
