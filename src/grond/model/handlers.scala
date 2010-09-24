package grond.model

/** A doctor present in the GROND's database.
 * @param id The Datastore identifier of the doctor. */
case class Doctor (val id: String) {}

/** A rating present in the GROND's database.
 * @param id The Datastore identifier of the rating. */
case class Rating (val id: String) {}
