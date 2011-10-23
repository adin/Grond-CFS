package grond.shared;

import grond.model.doctorUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;

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
  @Unindexed public transient List<String> ratings;

  @Unindexed @Serialized public LinkedHashMap<String, Integer> type;

  // Fields passed to GWT (see ServerIf#getDoctorsByRating).
  /** Not null if this doctor have a rating from the user currently logged in.<br>
   * Value contains "finished" if the current user's rating {@link DoctorRating#isFinished() isFinished}
   * (The actual check first happens in {@link doctorUtil#updateFromRatings} while filling {@link #ratings};
   * the field is then populated in {@link ServerIf#getDoctorsByRating}).<br>
   * This field is only available in GWT, do not use it in GAE. */
  @Transient public String _fromCurrentUser;

  @Override public int hashCode() {
    return id.intValue();
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof Doctor) {
      return id.equals(((Doctor) obj).id);
    } else return false;
  }
}
