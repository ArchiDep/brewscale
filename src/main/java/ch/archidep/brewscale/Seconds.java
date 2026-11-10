package ch.archidep.brewscale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Durations written as seconds, with a point as the decimal separator
 * whatever the locale.
 */
final class Seconds {

  /**
   * Rounds a duration to the given number of decimals: "1.001".
   */
  static String format(Duration duration, int decimals) {
    return seconds(duration)
      .setScale(decimals, RoundingMode.HALF_UP)
      .toPlainString();
  }

  /**
   * Writes a duration with as many decimals as it needs: "1", or "0.25".
   */
  static String plain(Duration duration) {
    return seconds(duration).stripTrailingZeros().toPlainString();
  }

  private static BigDecimal seconds(Duration duration) {
    return BigDecimal.valueOf(duration.toNanos(), 9);
  }

  private Seconds() {}
}
