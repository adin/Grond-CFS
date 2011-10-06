package grond.shared;

import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unindexed;

@Cached(expirationSeconds = 3600) public class Doctor {
  @Id public long id;

  public String country;
  public String region;
  public String city;
  public String firstName;
  public String lastName;

  // Fields automatically calculated from ratings, see `doctorUtil.updateFromRatings`.
  public int _fmSatisfaction;
  public int _cfsSatisfaction;
  @Unindexed public String _experience;
  public int _numberOfReviews;
  @Unindexed public int _averageCostLevel;
  /** Denormalized list of ratings, used to detect if the current user have rated the doctor. JSON. */
  //NOT USED?//@Unindexed public List<String> _ratings;

  @Unindexed @Embedded public Map<String, Integer> _type;
}
