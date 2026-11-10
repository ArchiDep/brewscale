package ch.archidep.brewscale;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BrewscaleTest {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();
  private final List<Integer> exits = new ArrayList<>();

  @Test
  void opensTheCafe() {
    var cafe = start(
      List.of("3"),
      Map.of(
        "BREWSCALE_HOST",
        "127.0.0.1",
        "BREWSCALE_PORT",
        "0",
        "BREWSCALE_BREW_TIME",
        "0"
      )
    );

    try {
      assertThat(cafe.isPresent(), is(true));
      assertThat(
        out.toString(StandardCharsets.UTF_8),
        is(
          equalTo(
            "Barista 3 listening on 127.0.0.1:" +
              cafe.orElseThrow().port() +
              ", brewing in 0s\n"
          )
        )
      );
      assertThat(err.toString(StandardCharsets.UTF_8), is(emptyString()));
      assertThat(exits, is(equalTo(emptyList())));
    } finally {
      cafe.ifPresent(Cafe::close);
    }
  }

  @Test
  void exitsWhenASettingIsInvalid() {
    var cafe = start(emptyList(), Map.of("BREWSCALE_PORT", "http"));

    assertThat(cafe, is(equalTo(Optional.empty())));
    assertThat(out.toString(StandardCharsets.UTF_8), is(emptyString()));
    assertThat(
      err.toString(StandardCharsets.UTF_8),
      is(
        equalTo(
          "BREWSCALE_PORT must be an integer between 0 and 65535, but got \"http\"\n"
        )
      )
    );
    assertThat(exits, contains(1));
  }

  /**
   * Runs the real main method in a JVM of its own, which is then stopped as
   * systemd stops it, with SIGTERM.
   *
   * <p>The child JVM is given this one's coverage agent, if any, so that main
   * counts as covered. JaCoCo only counts a method as covered once it returns,
   * so the café has to open: main never returns when a setting is invalid,
   * since it exits.
   */
  @Test
  void mainOpensTheCafeUntilStopped() throws Exception {
    var command = new ArrayList<String>();
    command.add(
      Path.of(System.getProperty("java.home"), "bin", "java").toString()
    );
    ManagementFactory.getRuntimeMXBean()
      .getInputArguments()
      .stream()
      .filter(argument -> argument.startsWith("-javaagent:"))
      .forEach(command::add);
    command.addAll(
      List.of(
        "-cp",
        System.getProperty("java.class.path"),
        Brewscale.class.getName(),
        "4"
      )
    );

    var builder = new ProcessBuilder(command);
    // Any error then shows up in the assertion on the first line.
    builder.redirectErrorStream(true);
    var env = builder.environment();
    env.keySet().removeIf(name -> name.startsWith("BREWSCALE_"));
    env.put("BREWSCALE_HOST", "127.0.0.1");
    env.put("BREWSCALE_PORT", "0");
    var process = builder.start();

    try {
      var output = process.inputReader(StandardCharsets.UTF_8);
      var startup = CompletableFuture.supplyAsync(() -> readLine(output)).get(
        30,
        TimeUnit.SECONDS
      );
      assertThat(
        startup,
        matchesPattern(
          "Barista 4 listening on 127\\.0\\.0\\.1:\\d+, brewing in 1s"
        )
      );

      process.destroy();
      assertThat(process.waitFor(30, TimeUnit.SECONDS), is(true));
      // 128 + 15, the number of SIGTERM.
      assertThat(process.exitValue(), is(equalTo(143)));
    } finally {
      process.destroyForcibly();
    }
  }

  private static String readLine(BufferedReader reader) {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Optional<Cafe> start(List<String> args, Map<String, String> env) {
    return Brewscale.start(
      args,
      env,
      new PrintStream(out, true, StandardCharsets.UTF_8),
      new PrintStream(err, true, StandardCharsets.UTF_8),
      exits::add
    );
  }
}
