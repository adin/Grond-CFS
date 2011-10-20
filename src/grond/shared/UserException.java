package grond.shared;

public class UserException extends Exception {
  private static final long serialVersionUID = 1L;
  protected int kind = -1;

  // Need a default constructor for GWT RPC.
  public UserException() {
  }

  public UserException(String message, int kind) {
    super(message);
    this.kind = kind;
  }

  public String toString() {
    return "UserException (" + getMessage() + ", " + kind + ")";
  }
}
