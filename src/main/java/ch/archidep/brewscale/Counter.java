package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * The counter where orders are taken and cups are handed out: the routes of
 * the application.
 */
final class Counter implements HttpHandler {

  /**
   * The largest order form body accepted, in bytes: far more than the longest
   * name, even with every character URL-encoded.
   */
  static final int MAX_BODY_SIZE = 1024;

  private final Barista barista;
  private final Duration brewTime;
  private final Waiting waiting;
  private final LongSupplier clock;
  private final PrintStream log;
  private final Pages pages;

  Counter(
    Configuration configuration,
    Waiting waiting,
    LongSupplier clock,
    PrintStream log
  ) {
    requireNonNull(configuration, "configuration");
    this.barista = configuration.barista();
    this.brewTime = configuration.brewTime();
    this.waiting = requireNonNull(waiting, "waiting");
    this.clock = requireNonNull(clock, "clock");
    this.log = requireNonNull(log, "log");
    this.pages = new Pages(barista);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      var method = exchange.getRequestMethod();
      switch (exchange.getRequestURI().getPath()) {
        case "/" -> {
          if (method.equals("GET")) {
            respond(exchange, 200, pages.form());
          } else {
            methodNotAllowed(exchange, "GET");
          }
        }
        case "/orders" -> {
          if (method.equals("POST")) {
            order(exchange);
          } else if (method.equals("GET")) {
            // A result page's address, entered again or shared: back to the
            // order form.
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(303, -1);
          } else {
            methodNotAllowed(exchange, "GET, POST");
          }
        }
        default -> respond(exchange, 404, pages.notFound());
      }
    }
  }

  private void order(HttpExchange exchange) throws IOException {
    var body = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);
    if (body.length > MAX_BODY_SIZE) {
      respond(exchange, 400, pages.form("Sorry, that order is too long."));
      return;
    }

    try {
      brew(exchange, Order.parse(new String(body, StandardCharsets.UTF_8)));
    } catch (InvalidOrderException e) {
      respond(exchange, 400, pages.form(e.getMessage()));
    }
  }

  private void brew(HttpExchange exchange, Order order) throws IOException {
    var start = clock.getAsLong();
    waiting.waitFor(brewTime);
    var brewed = Duration.ofNanos(clock.getAsLong() - start);

    log.println(
      barista.title() +
        " brewed " +
        order.drink().withArticle() +
        order
          .name()
          .map(name -> " for " + name)
          .orElse("") +
        " in " +
        Seconds.format(brewed, 3) +
        "s"
    );

    respond(exchange, 200, pages.result(order, brewed));
  }

  private void methodNotAllowed(HttpExchange exchange, String allowed)
    throws IOException {
    exchange.getResponseHeaders().set("Allow", allowed);
    respond(exchange, 405, pages.methodNotAllowed());
  }

  private static void respond(HttpExchange exchange, int status, String html)
    throws IOException {
    var bytes = html.getBytes(StandardCharsets.UTF_8);
    exchange
      .getResponseHeaders()
      .set("Content-Type", "text/html; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }
}
