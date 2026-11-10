package ch.archidep.brewscale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

  private static final String NOT_PRINTABLE =
    "Your name can only contain printable characters.";
  private static final String TOO_LONG =
    "Your name does not fit on the cup: 30 characters at most, please.";

  @Test
  void drinkAndName() throws InvalidOrderException {
    assertThat(
      Order.parse("drink=flat-white&name=Alice"),
      is(equalTo(new Order(Drink.FLAT_WHITE, Optional.of("Alice"))))
    );
  }

  @Test
  void drinkOnly() throws InvalidOrderException {
    assertThat(
      Order.parse("drink=latte"),
      is(equalTo(new Order(Drink.LATTE, Optional.empty())))
    );
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "drink=latte&name=",
      "drink=latte&name",
      "drink=latte&name=+%09+"
    }
  )
  void emptyName(String body) throws InvalidOrderException {
    assertThat(
      Order.parse(body),
      is(equalTo(new Order(Drink.LATTE, Optional.empty())))
    );
  }

  @Test
  void nameIsDecodedAndTrimmed() throws InvalidOrderException {
    assertThat(
      Order.parse("name=++Zo%C3%A9+%26+Ana+&drink=espresso").name(),
      is(equalTo(Optional.of("Zoé & Ana")))
    );
  }

  @Test
  void firstValueWins() throws InvalidOrderException {
    assertThat(
      Order.parse("drink=espresso&name=Alice&drink=latte&name=Bob"),
      is(equalTo(new Order(Drink.ESPRESSO, Optional.of("Alice"))))
    );
  }

  @Test
  void otherFieldsAreIgnored() throws InvalidOrderException {
    assertThat(
      Order.parse("size=large&drink=espresso&&sugar"),
      is(equalTo(new Order(Drink.ESPRESSO, Optional.empty())))
    );
  }

  @Test
  void nameOfMaximumLength() throws InvalidOrderException {
    assertThat(
      Order.parse("drink=latte&name=" + "a".repeat(29) + "%C3%A9").name(),
      is(equalTo(Optional.of("a".repeat(29) + "é")))
    );
  }

  @Test
  void nameLengthCountsCodePoints() throws InvalidOrderException {
    // 30 emoji are 60 UTF-16 code units, but 30 code points.
    var emoji = "%F0%9F%98%80".repeat(30);

    assertThat(
      Order.parse("drink=latte&name=" + emoji).name(),
      is(equalTo(Optional.of("😀".repeat(30))))
    );
  }

  @Test
  void nameTooLong() {
    var body = "drink=latte&name=" + "a".repeat(31);

    var e = assertThrows(InvalidOrderException.class, () -> Order.parse(body));

    assertThat(e.getMessage(), is(equalTo(TOO_LONG)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "Ali%0Ace", "Ali%00ce", "Ali%7Fce" })
  void nameWithControlCharacters(String name) {
    var e = assertThrows(InvalidOrderException.class, () ->
      Order.parse("drink=latte&name=" + name)
    );

    assertThat(e.getMessage(), is(equalTo(NOT_PRINTABLE)));
    assertThat(e.getCause(), is(instanceOf(IllegalArgumentException.class)));
  }

  @ParameterizedTest
  @ValueSource(strings = { "drink=tea", "drink=", "name=Alice", "" })
  void notOnTheMenu(String body) {
    var e = assertThrows(InvalidOrderException.class, () -> Order.parse(body));

    assertThat(e.getMessage(), is(equalTo("Sorry, that is not on the menu.")));
  }

  @Test
  void malformedEncodingIsKeptAsItIs() throws InvalidOrderException {
    assertThat(
      Order.parse("drink=latte&name=100%"),
      is(equalTo(new Order(Drink.LATTE, Optional.of("100%"))))
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " Alice", "Alice " })
  void cannotBeConstructedWithAnUntrimmedName(String name) {
    var cup = Optional.of(name);

    var e = assertThrows(IllegalArgumentException.class, () ->
      new Order(Drink.LATTE, cup)
    );

    assertThat(
      e.getMessage(),
      is(
        equalTo(
          "The name must be trimmed and not empty, but got \"" + name + "\""
        )
      )
    );
  }

  @Test
  void cannotBeConstructedWithANameTooLong() {
    var cup = Optional.of("a".repeat(31));

    var e = assertThrows(IllegalArgumentException.class, () ->
      new Order(Drink.LATTE, cup)
    );

    assertThat(e.getMessage(), is(equalTo(TOO_LONG)));
  }

  @Test
  void cannotBeConstructedWithControlCharacters() {
    var cup = Optional.of("Ali\nce");

    var e = assertThrows(IllegalArgumentException.class, () ->
      new Order(Drink.LATTE, cup)
    );

    assertThat(e.getMessage(), is(equalTo(NOT_PRINTABLE)));
  }
}
