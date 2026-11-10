package ch.archidep.brewscale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class WaitingTest {

  @Test
  void sleeps() {
    var start = System.nanoTime();

    Waiting.SLEEP.waitFor(Duration.ofMillis(50));

    assertThat(
      Duration.ofNanos(System.nanoTime() - start),
      is(greaterThanOrEqualTo(Duration.ofMillis(50)))
    );
  }

  @Test
  void stopsWhenInterruptedAndKeepsTheInterruption() {
    Thread.currentThread().interrupt();
    var start = System.nanoTime();

    Waiting.SLEEP.waitFor(Duration.ofMinutes(1));

    // Thread.interrupted() also clears the flag for the next tests.
    assertThat(Thread.interrupted(), is(true));
    assertThat(
      Duration.ofNanos(System.nanoTime() - start),
      is(lessThan(Duration.ofSeconds(30)))
    );
  }
}
