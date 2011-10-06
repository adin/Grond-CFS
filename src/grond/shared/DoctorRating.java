package grond.shared;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

@Cached(expirationSeconds = 3600) public class DoctorRating {
  @Id public long id;
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
  @Unindexed public String problem;

  public Date lastUpdate;

  // Indexed form fields.
  /** Indexed copy of `levels("satAfter")` */
  public int satAfter; // XXX: Update `satAfter` when it is updated in `levels`.

  // Unindexed form fields.
  @Unindexed public List<String> type;
  @Unindexed public String experience;
  @Unindexed public String averageCost;
  @Unindexed public String insurance;
  @Unindexed public String ripoff;
  @Unindexed public List<String> treatmentBreadth;
  @Unindexed public int actLevStart;
  @Unindexed public int actLevEnd;
  @Unindexed @Embedded public Map<String, Integer> levels;

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

  public boolean isFinished() {
    return satAfter > 0;
  }
}
