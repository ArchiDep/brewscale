package ch.archidep.brewscale;

import java.time.Duration;

/**
 * Waiting for the brew: the process waits and uses no CPU.
 */
@FunctionalInterface
interface Waiting {
  /**
   * Waits by putting the current thread to sleep, which blocks the server's
   * only thread for the duration.
   */
  Waiting SLEEP = duration -> {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  };

  void waitFor(Duration duration);
}
