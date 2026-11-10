package ch.archidep.brewscale;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CafeTest {

  private static final Duration BREW_TIME = Duration.ofSeconds(1);
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .connectTimeout(TIMEOUT)
    .build();

  private final ByteArrayOutputStream log = new ByteArrayOutputStream();
  private final AtomicLong now = new AtomicLong(42_000_000_000L);

  // Written by the server's thread, read by the test's.
  private final List<Duration> waits = new CopyOnWriteArrayList<>();

  // Brewing takes 1 ms longer than asked, as it does for real.
  private Waiting waiting = duration -> {
    waits.add(duration);
    now.addAndGet(duration.plusMillis(1).toNanos());
  };

  private @Nullable Cafe cafe;

  @AfterEach
  void closeTheCafe() {
    if (cafe != null) {
      cafe.close();
    }
  }

  @Test
  void logsTheAddressAtStartup() throws StartupException {
    var opened = open(Barista.numbered(2));

    assertThat(
      logLines(),
      contains(
        "Barista 2 listening on 127.0.0.1:" + opened.port() + ", brewing in 1s"
      )
    );
  }

  @Test
  void showsTheOrderForm() throws Exception {
    open(Barista.numbered(2));

    var response = get("/");

    assertThat(response.statusCode(), is(equalTo(200)));
    var page = html(response);
    var form = page.expectFirst("form");
    assertThat(form.attr("method"), is(equalTo("post")));
    assertThat(form.attr("action"), is(equalTo("/orders")));
    var menu = form.select("select[name=drink] option");
    assertThat(
      menu.eachAttr("value"),
      contains(
        "espresso",
        "americano",
        "cappuccino",
        "flat-white",
        "latte",
        "hot-chocolate"
      )
    );
    assertThat(
      menu.eachText(),
      contains(
        "Espresso",
        "Americano",
        "Cappuccino",
        "Flat white",
        "Latte",
        "Hot chocolate"
      )
    );
    assertThat(
      form.expectFirst("input[name=name]").attr("maxlength"),
      is(equalTo("30"))
    );
    assertThat(
      page.expectFirst("main h2").text(),
      is(equalTo("What would you like?"))
    );
    assertBarista(page, "Barista 2", "#1d5fa8");
    assertThat(page.select(".error"), hasSize(0));
    assertThat(page.select("script"), hasSize(1));
    assertThat(waits, is(equalTo(emptyList())));
  }

  @Test
  void showsThatTheOrderIsBrewing() throws Exception {
    open(Barista.numbered(2));

    var page = html(get("/"));

    // The script right after the form, written out as it is.
    var script = page.expectFirst("form + script").data();
    assertThat(script, containsString("form.inert = true;"));
    assertThat(script, containsString("button.textContent = 'Brewing…';"));
  }

  @Test
  void showsTheUnnumberedBarista() throws Exception {
    open(Barista.UNNUMBERED);

    var page = html(post("/orders", "drink=latte"));

    assertBarista(page, "The barista", Barista.COFFEE_BROWN);
    assertThat(
      page.expectFirst("#brewed").text(),
      is(equalTo("Brewed in 1.0s by the barista."))
    );
    assertThat(
      logLines().getLast(),
      is(equalTo("The barista brewed a latte in 1.001s"))
    );
  }

  @Test
  void brewsAnOrder() throws Exception {
    open(Barista.numbered(2));

    var response = post("/orders", "drink=flat-white&name=Alice");

    assertThat(response.statusCode(), is(equalTo(200)));
    var page = html(response);
    assertThat(
      page.select("main h2").eachText(),
      contains("Here is your flat white, Alice!", "Another one?")
    );
    var brewed = page.expectFirst("#brewed");
    assertThat(brewed.text(), is(equalTo("Brewed in 1.0s by barista 2.")));
    assertThat(brewed.attr("data-brew-seconds"), is(equalTo("1.001")));
    var queue = page.expectFirst("#queue");
    assertThat(queue.hasAttr("hidden"), is(true));
    assertThat(queue.text(), is(emptyString()));
    assertThat(page.select("form"), hasSize(1));
    assertThat(waits, contains(BREW_TIME));
    assertThat(
      logLines().getLast(),
      is(equalTo("Barista 2 brewed a flat white for Alice in 1.001s"))
    );
  }

  @Test
  void showsTheTimeSpentInTheQueue() throws Exception {
    open(Barista.numbered(2));

    var page = html(post("/orders", "drink=latte"));

    // The script at the end of the page, written out as it is, not read as a
    // template.
    var script = page.expectFirst("body > script").data();
    assertThat(
      script,
      containsString("performance.getEntriesByType('navigation')")
    );
    assertThat(
      script,
      containsString(
        "`You waited ${waited.toFixed(1)}s: the rest of the time, you were in the queue.`"
      )
    );
  }

  @Test
  void brewsAnOrderWithoutAName() throws Exception {
    open(Barista.numbered(2));

    var response = post("/orders", "drink=espresso&name=");

    assertThat(response.statusCode(), is(equalTo(200)));
    assertThat(
      html(response).expectFirst("main h2").text(),
      is(equalTo("Here is your espresso!"))
    );
    assertThat(
      logLines().getLast(),
      is(equalTo("Barista 2 brewed an espresso in 1.001s"))
    );
  }

  @Test
  void escapesTheName() throws Exception {
    open(Barista.numbered(2));

    var response = post(
      "/orders",
      "drink=latte&name=%3Cb%3E%22Tom%22+%26+%27Jo%27%3C%2Fb%3E"
    );

    assertThat(response.statusCode(), is(equalTo(200)));
    var heading = html(response).expectFirst("main h2");
    assertThat(
      heading.text(),
      is(equalTo("Here is your latte, <b>\"Tom\" & 'Jo'</b>!"))
    );
    assertThat(heading.select("b"), hasSize(0));
  }

  @Test
  void refusesAnUnknownDrink() throws Exception {
    open(Barista.numbered(2));

    var response = post("/orders", "drink=tea");

    assertRefused(response, "Sorry, that is not on the menu.");
  }

  @Test
  void refusesANameTooLong() throws Exception {
    open(Barista.numbered(2));

    var response = post("/orders", "drink=latte&name=" + "a".repeat(31));

    assertRefused(
      response,
      "Your name does not fit on the cup: 30 characters at most, please."
    );
  }

  @Test
  void acceptsTheLargestOrder() throws Exception {
    open(Barista.numbered(2));
    var body = "drink=latte&name=Alice&";

    var response = post(
      "/orders",
      body + "x".repeat(Counter.MAX_BODY_SIZE - body.length())
    );

    assertThat(response.statusCode(), is(equalTo(200)));
  }

  @Test
  void refusesAnOrderTooLong() throws Exception {
    open(Barista.numbered(2));
    var body = "drink=latte&name=Alice&";

    var response = post(
      "/orders",
      body + "x".repeat(Counter.MAX_BODY_SIZE - body.length() + 1)
    );

    assertRefused(response, "Sorry, that order is too long.");
  }

  @Test
  void answersNotFoundForAnyOtherPath() throws Exception {
    open(Barista.numbered(2));

    for (var response : List.of(
      get("/menu"),
      get("/orders/1"),
      post("/order", "drink=latte")
    )) {
      assertThat(response.statusCode(), is(equalTo(404)));
      var page = html(response);
      assertThat(
        page.expectFirst("main h2").text(),
        is(equalTo("There is nothing here"))
      );
      assertThat(page.expectFirst("main a").attr("href"), is(equalTo("/")));
      assertBarista(page, "Barista 2", "#1d5fa8");
    }
  }

  @Test
  void answersMethodNotAllowedOnTheForm() throws Exception {
    open(Barista.numbered(2));

    var response = post("/", "drink=latte");

    assertThat(response.statusCode(), is(equalTo(405)));
    assertThat(
      response.headers().firstValue("Allow"),
      is(equalTo(Optional.of("GET")))
    );
    var page = html(response);
    assertThat(
      page.expectFirst("main h2").text(),
      is(equalTo("That is not how to order here"))
    );
    assertBarista(page, "Barista 2", "#1d5fa8");
  }

  @Test
  void redirectsTheOrdersToTheForm() throws Exception {
    open(Barista.numbered(2));

    var response = get("/orders");

    assertThat(response.statusCode(), is(equalTo(303)));
    assertThat(
      response.headers().firstValue("Location"),
      is(equalTo(Optional.of("/")))
    );
    assertThat(response.body(), is(emptyString()));
    assertThat(waits, is(equalTo(emptyList())));
  }

  @Test
  void answersMethodNotAllowedOnTheOrders() throws Exception {
    open(Barista.numbered(2));

    var response = client.send(
      request("/orders").DELETE().build(),
      BodyHandlers.ofString()
    );

    assertThat(response.statusCode(), is(equalTo(405)));
    assertThat(
      response.headers().firstValue("Allow"),
      is(equalTo(Optional.of("GET, POST")))
    );
    html(response);
    assertThat(waits, is(equalTo(emptyList())));
  }

  /**
   * The regression test for the property the whole exercise relies on: while
   * a barista brews one order, the next one waits.
   */
  @Test
  void servesOneCustomerAtATime() throws Exception {
    var started = new Semaphore(0);
    var finish = new Semaphore(0);
    waiting = duration -> {
      started.release();
      finish.acquireUninterruptibly();
    };
    open(Barista.numbered(2));

    try {
      var first = postAsync("drink=latte&name=First");
      assertThat(
        started.tryAcquire(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
        is(true)
      );

      var second = postAsync("drink=latte&name=Second");
      assertThat(
        "The second order started brewing before the first was finished",
        started.tryAcquire(500, TimeUnit.MILLISECONDS),
        is(false)
      );

      finish.release();
      assertThat(
        html(first.get()).expectFirst("main h2").text(),
        is(equalTo("Here is your latte, First!"))
      );

      assertThat(
        started.tryAcquire(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
        is(true)
      );
      finish.release();
      assertThat(
        html(second.get()).expectFirst("main h2").text(),
        is(equalTo("Here is your latte, Second!"))
      );
    } finally {
      // Never leave the server's thread blocked, even when the test fails.
      finish.release(10);
    }
  }

  @Test
  void refusesAHostThatDoesNotResolve() {
    var configuration = configuration("nowhere.invalid", 0);

    var e = assertThrows(StartupException.class, () -> open(configuration));

    assertThat(
      e.getMessage(),
      is(
        equalTo(
          "BREWSCALE_HOST must be an IP address or a host name that resolves, but got \"nowhere.invalid\""
        )
      )
    );
  }

  @Test
  void explainsThatThePortIsAlreadyInUse() throws Exception {
    try (
      var socket = new ServerSocket(
        0,
        0,
        InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 })
      )
    ) {
      var port = socket.getLocalPort();
      var configuration = configuration("127.0.0.1", port);

      var e = assertThrows(StartupException.class, () -> open(configuration));

      assertThat(
        e.getMessage(),
        is(
          equalTo(
            "Cannot listen on 127.0.0.1:" +
              port +
              " because another process is already listening on port " +
              port +
              "."
          )
        )
      );
      assertThat(e.getCause(), is(instanceOf(BindException.class)));
    }
  }

  @Test
  void explainsThatTheAddressIsNotOneOfThisMachine() {
    // An address reserved for documentation, which no machine has.
    var configuration = configuration("192.0.2.1", 4202);

    var e = assertThrows(StartupException.class, () -> open(configuration));

    assertThat(
      e.getMessage(),
      is(
        equalTo(
          "Cannot listen on 192.0.2.1:4202 because 192.0.2.1 is not an address of this machine."
        )
      )
    );
  }

  @Test
  void givesTheSystemsReasonForAnyOtherFailure() {
    assertThat(
      Cafe.cannotListen("0.0.0.0", 80, new BindException("Permission denied")),
      is(equalTo("Cannot listen on 0.0.0.0:80: Permission denied"))
    );
  }

  private Cafe open(Barista barista) throws StartupException {
    return open(new Configuration(barista, "127.0.0.1", 0, BREW_TIME));
  }

  private Cafe open(Configuration configuration) throws StartupException {
    var opened = Cafe.open(
      configuration,
      waiting,
      now::get,
      new PrintStream(log, true, StandardCharsets.UTF_8)
    );
    cafe = opened;
    return opened;
  }

  private static Configuration configuration(String host, int port) {
    return new Configuration(Barista.numbered(2), host, port, BREW_TIME);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(request(path).GET().build(), BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return client.send(postRequest(path, body), BodyHandlers.ofString());
  }

  private CompletableFuture<HttpResponse<String>> postAsync(String body) {
    return client.sendAsync(
      postRequest("/orders", body),
      BodyHandlers.ofString()
    );
  }

  private HttpRequest postRequest(String path, String body) {
    return request(path)
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(BodyPublishers.ofString(body))
      .build();
  }

  private HttpRequest.Builder request(String path) {
    var port = requireNonNull(cafe, "cafe").port();
    return HttpRequest.newBuilder(
      URI.create("http://127.0.0.1:" + port + path)
    ).timeout(TIMEOUT);
  }

  private List<String> logLines() {
    return log.toString(StandardCharsets.UTF_8).lines().toList();
  }

  private void assertRefused(HttpResponse<String> response, String error) {
    assertThat(response.statusCode(), is(equalTo(400)));
    var page = html(response);
    assertThat(page.expectFirst(".error").text(), is(equalTo(error)));
    assertThat(
      page.expectFirst("main h2").text(),
      is(equalTo("What would you like?"))
    );
    assertThat(page.select("form"), hasSize(1));
    assertThat(waits, is(equalTo(emptyList())));
    // Only the startup line.
    assertThat(logLines(), hasSize(1));
  }

  /**
   * Checks that the response is an HTML page, and parses it.
   */
  private static Document html(HttpResponse<String> response) {
    assertThat(
      response.headers().firstValue("Content-Type"),
      is(equalTo(Optional.of("text/html; charset=utf-8")))
    );
    var page = Jsoup.parse(response.body());
    assertThat(page.title(), is(equalTo("Brewscale")));
    return page;
  }

  private static void assertBarista(
    Document page,
    String title,
    String colour
  ) {
    assertThat(page.expectFirst("header p").text(), is(equalTo(title)));
    assertThat(page.body().attr("style"), is(equalTo("--barista: " + colour)));
  }
}
