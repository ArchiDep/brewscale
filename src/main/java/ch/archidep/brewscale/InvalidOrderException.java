package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

/**
 * Raised when an order cannot be brewed, with a message for the customer.
 */
final class InvalidOrderException extends Exception {

  private static final long serialVersionUID = 1L;

  InvalidOrderException(String message) {
    super(requireNonNull(message, "message"));
  }

  InvalidOrderException(String message, Throwable cause) {
    super(requireNonNull(message, "message"), requireNonNull(cause, "cause"));
  }

  /**
   * The message for the customer, which every constructor requires.
   */
  @Override
  public String getMessage() {
    return requireNonNull(super.getMessage());
  }
}
