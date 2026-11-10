package ch.archidep.brewscale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SecondsTest {

  @Test
  void format() {
    assertThat(
      Seconds.format(Duration.ofMillis(1001), 3),
      is(equalTo("1.001"))
    );
    assertThat(Seconds.format(Duration.ofMillis(1001), 1), is(equalTo("1.0")));
    assertThat(Seconds.format(Duration.ofMillis(1050), 1), is(equalTo("1.1")));
    assertThat(Seconds.format(Duration.ZERO, 3), is(equalTo("0.000")));
  }

  @Test
  void plain() {
    assertThat(Seconds.plain(Duration.ofSeconds(1)), is(equalTo("1")));
    assertThat(Seconds.plain(Duration.ofMillis(250)), is(equalTo("0.25")));
    assertThat(Seconds.plain(Duration.ZERO), is(equalTo("0")));
    assertThat(Seconds.plain(Duration.ofMinutes(2)), is(equalTo("120")));
  }
}
