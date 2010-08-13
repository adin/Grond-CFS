package grond.shared;

import java.util.Arrays;
import java.util.List;

/**
 * The list of supported countries.<br>
 * Gives our program some knowledge about the countries.
 */
public class Countries {
  public static class Country {
    public final String name;
    /** See http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3 */
    public final String id;

    public Country(String name, String id) {
      this.name = name;
      this.id = id;
      assert id.length() == 3 : id;
    }
  }
  /** Countries supported by our program. */
  public final static List<Country> COUNTRIES = Arrays.asList (
    new Country ("United States", "usa"),
    new Country ("Canada", "can"),
    new Country ("United Kingdom", "gbr"),
    new Country ("Australia", "aus"),
    new Country ("New Zealand", "nzl"),
    new Country ("Spain", "esp")
  );
}
