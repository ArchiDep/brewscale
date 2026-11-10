package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * The café: one barista behind the counter, listening for orders.
 *
 * <p>The JDK's HTTP server handles every request on the single thread that
 * {@link HttpServer#start()} creates, as long as no executor is set. Brewing
 * blocks that thread, so a barista serves one customer at a time while the
 * others wait in the queue. That is the whole point of the application: never
 * set an executor.
 */
final class Cafe implements AutoCloseable {

  private final HttpServer server;

  private Cafe(HttpServer server) {
    this.server = requireNonNull(server, "server");
  }

  /**
   * Opens the café and logs the address it listens on.
   *
   * @param waiting how to wait for each brew
   * @param clock a source of nanoseconds to measure the brewing time
   * @param log where to log the café's activity
   */
  static Cafe open(
    Configuration configuration,
    Waiting waiting,
    LongSupplier clock,
    PrintStream log
  ) throws StartupException {
    var server = listen(configuration.host(), configuration.port());
    server.createContext("/", new Counter(configuration, waiting, clock, log));
    server.start();

    var cafe = new Cafe(server);
    log.println(
      configuration.barista().title() +
        " listening on " +
        configuration.host() +
        ":" +
        cafe.port() +
        ", brewing in " +
        Seconds.plain(configuration.brewTime()) +
        "s"
    );

    return cafe;
  }

  /**
   * Explains why the café cannot listen on an address, as clearly as the
   * operating system's reason allows.
   */
  static String cannotListen(String host, int port, IOException cause) {
    var reason = Objects.requireNonNullElse(cause.getMessage(), "");
    var cannot = "Cannot listen on " + host + ":" + port;

    if (reason.contains("already in use")) {
      return (
        cannot +
        " because another process is already listening on port " +
        port +
        "."
      );
    } else if (reason.contains("assign requested address")) {
      return (
        cannot + " because " + host + " is not an address of this machine."
      );
    }

    return cannot + ": " + reason;
  }

  /**
   * The port the café listens on, which the system picks when the configured
   * port is 0.
   */
  int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private static HttpServer listen(String host, int port)
    throws StartupException {
    var address = new InetSocketAddress(host, port);
    if (address.isUnresolved()) {
      throw new StartupException(
        Configuration.HOST_VARIABLE +
          " must be an IP address or a host name that resolves, but got \"" +
          host +
          "\""
      );
    }

    try {
      return HttpServer.create(address, 0);
    } catch (IOException e) {
      throw new StartupException(cannotListen(host, port, e), e);
    }
  }
}
