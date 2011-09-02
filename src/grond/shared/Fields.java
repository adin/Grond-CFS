package grond.shared;

import java.util.Arrays;
import java.util.List;

/**
 * Shared information about database fields.
 */
public class Fields {
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
}
