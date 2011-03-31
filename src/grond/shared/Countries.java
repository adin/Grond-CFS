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

    /** List all regions of the given country. Used in region selector. */
    public String[] getRegions() {
      // Obtaining region list from AmMap files:
      //   aptitude install libxml-xpath-perl
      //   perl -e 'use XML::XPath; my $xp = XML::XPath->new(filename => "workspace/Grond/war/ammap/_countries/usa/ammap_data.xml"); print "\"" . join ("\",\"", map {$_->getAttribute("title")} ($xp->find("//area")->get_nodelist)) . "\""'
      if (id.equals("usa"))
        return new String[] { "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
            "Connecticut", "Delaware", "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho",
            "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland",
            "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska",
            "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina",
            "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
            "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
            "West Virginia", "Wisconsin", "Wyoming" };
      throw new RuntimeException("No region list for this country: " + id);
    }
  }
  /** Countries supported by our program. */
  public final static List<Country> COUNTRIES = Arrays.asList(new Country("United States", "usa"),
      new Country("Canada", "can"), new Country("United Kingdom", "gbr"), new Country("Australia", "aus"),
      new Country("New Zealand", "nzl"), new Country("Spain", "esp"));

  public static Country getCountry(final String countryId) {
    for (final Country country : COUNTRIES)
      if (country.id.equals(countryId)) return country;
    throw new RuntimeException("Unknown country id: " + countryId);
  }
}
