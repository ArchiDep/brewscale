package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * The menu. Every drink takes the same time to brew.
 */
enum Drink {
  ESPRESSO("espresso", "espresso", "an"),
  AMERICANO("americano", "americano", "an"),
  CAPPUCCINO("cappuccino", "cappuccino", "a"),
  FLAT_WHITE("flat-white", "flat white", "a"),
  LATTE("latte", "latte", "a"),
  HOT_CHOCOLATE("hot-chocolate", "hot chocolate", "a");

  private final String value;
  private final String label;
  private final String article;

  Drink(String value, String label, String article) {
    this.value = requireNonNull(value, "value");
    this.label = requireNonNull(label, "label");
    this.article = requireNonNull(article, "article");
  }

  /**
   * The value the order form posts: "flat-white".
   */
  String value() {
    return value;
  }

  /**
   * The drink's name within a sentence: "flat white".
   */
  String label() {
    return label;
  }

  /**
   * The drink's name on the menu: "Flat white".
   */
  String menuLabel() {
    return Character.toUpperCase(label.charAt(0)) + label.substring(1);
  }

  /**
   * The drink's name with its indefinite article: "an espresso".
   */
  String withArticle() {
    return article + " " + label;
  }

  static Optional<Drink> fromValue(String value) {
    return Arrays.stream(values())
      .filter(drink -> drink.value.equals(value))
      .findFirst();
  }
}
