package ch.archidep.brewscale;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Brewscale, a café whose baristas take a while to brew each coffee.
 *
 * <p>Each running instance of the application is one barista, serving one
 * customer at a time.
 */
public final class Brewscale {

  private Brewscale() {}

  /**
   * Opens the café with the barista number given as the optional argument and
   * the settings found in the environment.
   */
  public static void main(String[] args) {
    start(
      List.of(args),
      System.getenv(),
      System.out,
      System.err,
      System::exit,
      Brewscale::onSigterm
    );
  }

  /**
   * Opens the café, or reports why it cannot be opened on the error stream and
   * exits with code 1.
   *
   * <p>Once the settings are valid, it exits with code 0 when it is asked to
   * stop, through {@code onStop}. Without that, the JVM would exit with code
   * 143 on SIGTERM, which is how systemd stops it, and systemd would count
   * every stop as a failure.
   *
   * @param onStop registers what to do when the café is asked to stop
   */
  static Optional<Cafe> start(
    List<String> args,
    Map<String, String> env,
    PrintStream out,
    PrintStream err,
    IntConsumer exit,
    Consumer<Runnable> onStop
  ) {
    try {
      var configuration = Configuration.parse(args, env);
      // Before the café says it is open, so that it can be stopped from then
      // on.
      onStop.accept(() -> exit.accept(0));
      return Optional.of(
        Cafe.open(configuration, Waiting.SLEEP, System::nanoTime, out)
      );
    } catch (StartupException e) {
      err.println(e.getMessage());
      exit.accept(1);
      return Optional.empty();
    }
  }

  /**
   * Runs an action when the process receives SIGTERM, instead of the JVM's
   * default of exiting with code 143.
   *
   * @return the handler it replaces
   */
  static SignalHandler onSigterm(Runnable action) {
    return Signal.handle(new Signal("TERM"), signal -> action.run());
  }
}
