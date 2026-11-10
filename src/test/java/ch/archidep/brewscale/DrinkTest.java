package ch.archidep.brewscale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DrinkTest {

  @Test
  void menu() {
    assertThat(
      Arrays.stream(Drink.values()).map(Drink::value).toList(),
      contains(
        "espresso",
        "americano",
        "cappuccino",
        "flat-white",
        "latte",
        "hot-chocolate"
      )
    );
  }

  @Test
  void labelsAndArticles() {
    assertThat(Drink.FLAT_WHITE.label(), is(equalTo("flat white")));
    assertThat(Drink.FLAT_WHITE.withArticle(), is(equalTo("a flat white")));
    assertThat(Drink.ESPRESSO.withArticle(), is(equalTo("an espresso")));
    assertThat(Drink.AMERICANO.withArticle(), is(equalTo("an americano")));
    assertThat(
      Drink.HOT_CHOCOLATE.withArticle(),
      is(equalTo("a hot chocolate"))
    );
  }

  @Test
  void fromValue() {
    assertThat(
      Drink.fromValue("flat-white"),
      is(equalTo(Optional.of(Drink.FLAT_WHITE)))
    );
    assertThat(Drink.fromValue("FLAT_WHITE"), is(equalTo(Optional.empty())));
    assertThat(Drink.fromValue("tea"), is(equalTo(Optional.empty())));
  }
}
