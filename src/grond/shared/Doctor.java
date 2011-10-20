package grond.shared;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.annotation.Unindexed;

@Cached(expirationSeconds = 3600) public class Doctor implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id public Long id;

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
  /** Denormalized list of ratings, used to detect if the current user have rated the doctor.<br>
   * Values are JSON-encoded.<br>
   * See `ServerImpl.getDoctorsByRating`. */
  @Unindexed public List<String> ratings;

  @Unindexed @Serialized public LinkedHashMap<String, Integer> type;
}
