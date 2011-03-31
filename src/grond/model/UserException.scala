package grond.model

case class UserException (message: String, kind: Int) extends Exception (message) {
  override def toString = "UserException (" + message + ", " + kind + ")"
}
