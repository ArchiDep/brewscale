package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

/**
 * Raised when the café cannot open, with a message for whoever started it.
 */
final class StartupException extends Exception {

  private static final long serialVersionUID = 1L;

  StartupException(String message) {
    super(requireNonNull(message, "message"));
  }

  StartupException(String message, Throwable cause) {
    super(requireNonNull(message, "message"), requireNonNull(cause, "cause"));
  }

  /**
   * The message for whoever started the café, which every constructor
   * requires.
   */
  @Override
  public String getMessage() {
    return requireNonNull(super.getMessage());
  }
}
