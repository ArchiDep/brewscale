package ch.archidep.brewscale;

import java.util.List;

/**
 * One instance of the application, identified by an optional number and the
 * colour it picks.
 */
final class Barista {

  /**
   * The café's single barista, when no number is given.
   */
  static final Barista UNNUMBERED = new Barista(0);

  /**
   * Colours that each carry white text legibly, picked in turn by the
   * barista numbers.
   */
  static final List<String> COLOURS = List.of(
    "#b3261e",
    "#1d5fa8",
    "#2e7d32",
    "#7b3fa0",
    "#a84a0b",
    "#00707a",
    "#a3195b",
    "#4a5568"
  );

  static final String COFFEE_BROWN = "#6f4e37";

  // 0 for the unnumbered barista, at least 1 otherwise.
  private final int number;

  private Barista(int number) {
    this.number = number;
  }

  /**
   * @throws IllegalArgumentException if the number is not positive
   */
  static Barista numbered(int number) {
    if (number < 1) {
      throw new IllegalArgumentException(invalidNumber(String.valueOf(number)));
    }

    return new Barista(number);
  }

  static String invalidNumber(String value) {
    return (
      "The barista number must be a positive integer, but got \"" + value + "\""
    );
  }

  /**
   * The barista's name at the start of a sentence: "Barista 2", or "The
   * barista".
   */
  String title() {
    return number == 0 ? "The barista" : "Barista " + number;
  }

  /**
   * The barista's name within a sentence: "barista 2", or "the barista".
   */
  String name() {
    return number == 0 ? "the barista" : "barista " + number;
  }

  String colour() {
    return number == 0
      ? COFFEE_BROWN
      : COLOURS.get((number - 1) % COLOURS.size());
  }
}
