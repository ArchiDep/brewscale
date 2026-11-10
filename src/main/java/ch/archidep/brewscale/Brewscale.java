package ch.archidep.brewscale;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

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
    start(List.of(args), System.getenv(), System.out, System.err, System::exit);
  }

  /**
   * Opens the café, or reports why it cannot be opened on the error stream and
   * exits with code 1.
   */
  static Optional<Cafe> start(
    List<String> args,
    Map<String, String> env,
    PrintStream out,
    PrintStream err,
    IntConsumer exit
  ) {
    try {
      var configuration = Configuration.parse(args, env);
      return Optional.of(
        Cafe.open(configuration, Waiting.SLEEP, System::nanoTime, out)
      );
    } catch (StartupException e) {
      err.println(e.getMessage());
      exit.accept(1);
      return Optional.empty();
    }
  }
}
