package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.WWWFormCodec;

/**
 * A drink, and the name to write on the cup if the customer gave one.
 */
record Order(Drink drink, Optional<String> name) {
  static final int MAX_NAME_LENGTH = 30;

  /**
   * @throws IllegalArgumentException if the name cannot go on the cup, with a
   *     message for the customer
   */
  Order {
    requireNonNull(drink, "drink");
    requireNonNull(name, "name");
    name.ifPresent(Order::checkName);
  }

  /**
   * Reads an order from the body of the order form, URL-encoded. When a field
   * is given more than once, the first value counts.
   */
  static Order parse(String body) throws InvalidOrderException {
    var fields = WWWFormCodec.parse(body, StandardCharsets.UTF_8);

    var drink = field(fields, "drink")
      .flatMap(Drink::fromValue)
      .orElseThrow(() ->
        new InvalidOrderException("Sorry, that is not on the menu.")
      );

    var name = field(fields, "name")
      .map(String::strip)
      .filter(value -> !value.isEmpty());

    try {
      return new Order(drink, name);
    } catch (IllegalArgumentException e) {
      throw new InvalidOrderException(
        Objects.requireNonNullElse(e.getMessage(), e.toString()),
        e
      );
    }
  }

  private static Optional<String> field(
    List<NameValuePair> fields,
    String name
  ) {
    return fields
      .stream()
      .filter(field -> field.getName().equals(name))
      // A field without "=" has no value.
      .map(field -> Objects.requireNonNullElse(field.getValue(), ""))
      .findFirst();
  }

  private static void checkName(String name) {
    if (name.isEmpty() || !name.equals(name.strip())) {
      throw new IllegalArgumentException(
        "The name must be trimmed and not empty, but got \"" + name + "\""
      );
    } else if (name.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException(
        "Your name can only contain printable characters."
      );
    } else if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
        "Your name does not fit on the cup: " +
          MAX_NAME_LENGTH +
          " characters at most, please."
      );
    }
  }
}
