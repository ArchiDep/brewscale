package ch.archidep.brewscale;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationTest {

  @Test
  void defaults() throws StartupException {
    var configuration = Configuration.parse(emptyList(), emptyMap());

    assertThat(configuration.barista().title(), is(equalTo("The barista")));
    assertThat(configuration.host(), is(equalTo("0.0.0.0")));
    assertThat(configuration.port(), is(equalTo(3000)));
    assertThat(configuration.brewTime(), is(equalTo(Duration.ofSeconds(1))));
  }

  @Test
  void everySetting() throws StartupException {
    var configuration = Configuration.parse(
      List.of("2"),
      Map.of(
        "BREWSCALE_HOST",
        "127.0.0.1",
        "BREWSCALE_PORT",
        "4202",
        "BREWSCALE_BREW_TIME",
        "0.25"
      )
    );

    assertThat(configuration.barista().title(), is(equalTo("Barista 2")));
    assertThat(configuration.host(), is(equalTo("127.0.0.1")));
    assertThat(configuration.port(), is(equalTo(4202)));
    assertThat(configuration.brewTime(), is(equalTo(Duration.ofMillis(250))));
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "65535" })
  void portLimits(String port) throws StartupException {
    var configuration = Configuration.parse(
      emptyList(),
      Map.of("BREWSCALE_PORT", port)
    );

    assertThat(configuration.port(), is(equalTo(Integer.parseInt(port))));
  }

  @Test
  void brewTimeOfZero() throws StartupException {
    var configuration = Configuration.parse(
      emptyList(),
      Map.of("BREWSCALE_BREW_TIME", "0")
    );

    assertThat(configuration.brewTime(), is(equalTo(Duration.ZERO)));
  }

  @Test
  void longestBrewTime() throws StartupException {
    var configuration = Configuration.parse(
      emptyList(),
      Map.of("BREWSCALE_BREW_TIME", "999999999.999999999")
    );

    assertThat(
      configuration.brewTime(),
      is(equalTo(Duration.ofSeconds(999_999_999, 999_999_999)))
    );
  }

  @Test
  void largestBaristaNumber() throws StartupException {
    var configuration = Configuration.parse(List.of("999999999"), emptyMap());

    assertThat(
      configuration.barista().title(),
      is(equalTo("Barista 999999999"))
    );
  }

  @Test
  void tooManyArguments() {
    var e = assertThrows(StartupException.class, () ->
      Configuration.parse(List.of("1", "2"), emptyMap())
    );

    assertThat(
      e.getMessage(),
      is(
        equalTo("Expected at most one argument, the barista number, but got 2")
      )
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "+1", "1.5", "one", "", "1000000000" })
  void invalidBaristaNumber(String number) {
    var e = assertThrows(StartupException.class, () ->
      Configuration.parse(List.of(number), emptyMap())
    );

    assertThat(
      e.getMessage(),
      is(
        equalTo(
          "The barista number must be a positive integer, but got \"" +
            number +
            "\""
        )
      )
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " " })
  void emptyHost(String host) {
    var e = assertThrows(StartupException.class, () ->
      Configuration.parse(emptyList(), Map.of("BREWSCALE_HOST", host))
    );

    assertThat(e.getMessage(), is(equalTo("BREWSCALE_HOST must not be empty")));
    assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "65536", "99999", "100000", "-1", "port", "" })
  void invalidPort(String port) {
    var e = assertThrows(StartupException.class, () ->
      Configuration.parse(emptyList(), Map.of("BREWSCALE_PORT", port))
    );

    assertThat(e.getMessage(), is(equalTo(invalidPortMessage(port))));
  }

  @ParameterizedTest
  @ValueSource(
    strings = { "-1", "1.", ".5", "1,5", "1e3", "one", "", "1000000000" }
  )
  void invalidBrewTime(String brewTime) {
    var e = assertThrows(StartupException.class, () ->
      Configuration.parse(emptyList(), Map.of("BREWSCALE_BREW_TIME", brewTime))
    );

    assertThat(e.getMessage(), is(equalTo(invalidBrewTimeMessage(brewTime))));
  }

  @Test
  void cannotBeConstructedWithANegativePort() {
    var e = assertThrows(IllegalArgumentException.class, () ->
      configuration("127.0.0.1", -1, Duration.ZERO)
    );

    assertThat(e.getMessage(), is(equalTo(invalidPortMessage("-1"))));
  }

  @Test
  void cannotBeConstructedWithAPortTooLarge() {
    var e = assertThrows(IllegalArgumentException.class, () ->
      configuration("127.0.0.1", 65536, Duration.ZERO)
    );

    assertThat(e.getMessage(), is(equalTo(invalidPortMessage("65536"))));
  }

  @Test
  void cannotBeConstructedWithANegativeBrewTime() {
    var brewTime = Duration.ofMillis(-500);

    var e = assertThrows(IllegalArgumentException.class, () ->
      configuration("127.0.0.1", 0, brewTime)
    );

    assertThat(e.getMessage(), is(equalTo(invalidBrewTimeMessage("-0.5"))));
  }

  @Test
  void cannotBeConstructedWithAnEmptyHost() {
    var e = assertThrows(IllegalArgumentException.class, () ->
      configuration("", 0, Duration.ZERO)
    );

    assertThat(e.getMessage(), is(equalTo("BREWSCALE_HOST must not be empty")));
  }

  private static Configuration configuration(
    String host,
    int port,
    Duration brewTime
  ) {
    return new Configuration(Barista.UNNUMBERED, host, port, brewTime);
  }

  private static String invalidPortMessage(String port) {
    return (
      "BREWSCALE_PORT must be an integer between 0 and 65535, but got \"" +
      port +
      "\""
    );
  }

  private static String invalidBrewTimeMessage(String brewTime) {
    return (
      "BREWSCALE_BREW_TIME must be a number of seconds of at least 0, such as 1 or 0.5, but got \"" +
      brewTime +
      "\""
    );
  }
}
