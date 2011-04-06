package grond.client;

import java.util.logging.Logger;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;

/**
 * Collect practicioner information from a user.
 */
public class RatingForm {
  final Grond grond;
  final String countryId;
  final String condition;
  final String ratingId;
  final Panel panel;
  /** If you have the rating, set it there to save us from the RPC roundtrip. */
  public JSONObject rating;
  int step = 1;

  RatingForm(Grond grond, String countryId, String condition, String ratingId, Panel panel) {
    this.grond = grond;
    this.countryId = countryId;
    this.condition = condition;
    this.ratingId = ratingId;
    this.panel = panel;
  }

  void addToPanel() {
    final HTML doctorInfo = new HTML("<img src='" + Grond.ajaxLoaderHorisontal1() + "' />");
    panel.add(doctorInfo);

    final Grond.Callback<JSONObject> render = grond.new Callback<JSONObject>() {
      public void onSuccess(final JSONObject $rating) {
        rating = $rating;

        // Security check: signed in user should be the same as the rating's owner.
        final String ratingUser = rating.get("user").isString().stringValue();
        final String gaeUser = grond.currentUser == null ? null : grond.currentUser.getId();
        Logger.getLogger("RatingForm").info("rating: " + rating);
        Logger.getLogger("RatingForm").info("ratingUser: " + ratingUser);
        Logger.getLogger("RatingForm").info("gaeUser: " + gaeUser);
        if (gaeUser == null || !gaeUser.equals(ratingUser)) {
          panel.add(new HTML("Internal error: This rating belongs to a different user."
              + " Make sure you are signed in properly."));
          throw new RuntimeException("Expected user: " + ratingUser + "; got: " + gaeUser);
        }

        final String name = rating.get("doctor").isObject().get("firstName").isString().stringValue();
        final String surname = rating.get("doctor").isObject().get("lastName").isString().stringValue();
        doctorInfo.setHTML("Rating of: " + name + ' ' + surname + ".");

        if (step == 1) firstStep();
        else throw new RuntimeException("Unknown step: " + step);
      }
    };

    // Reuse the rating if it is already fetched.
    if (rating != null) render.onSuccess(rating);
    else grond.getGae().getRating(ratingId, render);
  }

  protected void firstStep() {
    panel.add(new HTML("TYPE OF HEALTH PROFESSIONAL"));
    panel.add(new CheckBox("Family Physician"));
    panel.add(new CheckBox("General Internal Medicine"));
  }
}
