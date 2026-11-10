package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The settings of one barista, read once from the arguments and the
 * environment.
 */
record Configuration(
  Barista barista,
  String host,
  int port,
  Duration brewTime
) {
  static final String HOST_VARIABLE = "BREWSCALE_HOST";
  static final String PORT_VARIABLE = "BREWSCALE_PORT";
  static final String BREW_TIME_VARIABLE = "BREWSCALE_BREW_TIME";

  static final int MAX_PORT = 65535;

  private static final String DEFAULT_HOST = "0.0.0.0";
  private static final String DEFAULT_PORT = "3000";
  private static final String DEFAULT_BREW_TIME = "1";

  // At most 9 digits, so that every barista number fits in an int, and the
  // nanoseconds of every brewing time fit in a long.
  private static final Pattern BARISTA_NUMBER_FORMAT = Pattern.compile(
    "\\d{1,9}"
  );
  private static final Pattern PORT_FORMAT = Pattern.compile("\\d{1,5}");
  private static final Pattern BREW_TIME_FORMAT = Pattern.compile(
    "\\d{1,9}(\\.\\d{1,9})?"
  );

  /**
   * @throws IllegalArgumentException if a setting is invalid, with a message
   *     naming it
   */
  Configuration {
    requireNonNull(barista, "barista");
    requireNonNull(host, "host");
    requireNonNull(brewTime, "brewTime");

    if (host.isBlank()) {
      throw new IllegalArgumentException(HOST_VARIABLE + " must not be empty");
    } else if (port < 0 || port > MAX_PORT) {
      throw new IllegalArgumentException(invalidPort(String.valueOf(port)));
    } else if (brewTime.isNegative()) {
      throw new IllegalArgumentException(
        invalidBrewTime(Seconds.plain(brewTime))
      );
    }
  }

  static Configuration parse(List<String> args, Map<String, String> env)
    throws StartupException {
    try {
      return new Configuration(
        parseBarista(args),
        env.getOrDefault(HOST_VARIABLE, DEFAULT_HOST),
        parsePort(env.getOrDefault(PORT_VARIABLE, DEFAULT_PORT)),
        parseBrewTime(env.getOrDefault(BREW_TIME_VARIABLE, DEFAULT_BREW_TIME))
      );
    } catch (IllegalArgumentException e) {
      // The constructors reject invalid values with a message naming the
      // setting.
      throw new StartupException(
        Objects.requireNonNullElse(e.getMessage(), e.toString()),
        e
      );
    }
  }

  private static Barista parseBarista(List<String> args)
    throws StartupException {
    if (args.isEmpty()) {
      return Barista.UNNUMBERED;
    } else if (args.size() > 1) {
      throw new StartupException(
        "Expected at most one argument, the barista number, but got " +
          args.size()
      );
    }

    var value = args.getFirst();
    if (!BARISTA_NUMBER_FORMAT.matcher(value).matches()) {
      throw new StartupException(Barista.invalidNumber(value));
    }

    return Barista.numbered(Integer.parseInt(value));
  }

  private static int parsePort(String value) throws StartupException {
    if (!PORT_FORMAT.matcher(value).matches()) {
      throw new StartupException(invalidPort(value));
    }

    return Integer.parseInt(value);
  }

  private static Duration parseBrewTime(String value) throws StartupException {
    if (!BREW_TIME_FORMAT.matcher(value).matches()) {
      throw new StartupException(invalidBrewTime(value));
    }

    return Duration.ofNanos(
      new BigDecimal(value).movePointRight(9).longValueExact()
    );
  }

  private static String invalidPort(String value) {
    return (
      PORT_VARIABLE +
      " must be an integer between 0 and " +
      MAX_PORT +
      ", but got \"" +
      value +
      "\""
    );
  }

  private static String invalidBrewTime(String value) {
    return (
      BREW_TIME_VARIABLE +
      " must be a number of seconds of at least 0, such as 1 or 0.5, but got \"" +
      value +
      "\""
    );
  }
}
