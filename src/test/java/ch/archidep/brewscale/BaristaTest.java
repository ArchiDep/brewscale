package ch.archidep.brewscale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BaristaTest {

  @Test
  void numbered() {
    var barista = Barista.numbered(2);

    assertThat(barista.title(), is(equalTo("Barista 2")));
    assertThat(barista.name(), is(equalTo("barista 2")));
    assertThat(barista.colour(), is(equalTo("#1d5fa8")));
  }

  @Test
  void unnumbered() {
    var barista = Barista.UNNUMBERED;

    assertThat(barista.title(), is(equalTo("The barista")));
    assertThat(barista.name(), is(equalTo("the barista")));
    assertThat(barista.colour(), is(equalTo(Barista.COFFEE_BROWN)));
  }

  @Test
  void coloursCycle() {
    assertThat(Barista.COLOURS, hasSize(8));
    assertThat(Barista.numbered(1).colour(), is(equalTo("#b3261e")));
    assertThat(Barista.numbered(8).colour(), is(equalTo("#4a5568")));
    assertThat(Barista.numbered(9).colour(), is(equalTo("#b3261e")));
    assertThat(Barista.numbered(10).colour(), is(equalTo("#1d5fa8")));
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void cannotBeNumberedBelowOne(int number) {
    var e = assertThrows(IllegalArgumentException.class, () ->
      Barista.numbered(number)
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
}
