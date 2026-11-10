package ch.archidep.brewscale;

import static java.util.Objects.requireNonNull;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * The HTML pages, each served by and showing the colour of one barista,
 * rendered from the jte templates in src/main/jte. They have no external
 * resources: the CSS and the only script are inline.
 */
final class Pages {

  /**
   * The templates, compiled into classes of this package during the build.
   */
  private static final TemplateEngine TEMPLATES =
    TemplateEngine.createPrecompiled(
      null,
      ContentType.Html,
      null,
      Pages.class.getPackageName()
    );

  private static final String BARISTA = "barista";

  private final Barista barista;

  Pages(Barista barista) {
    this.barista = requireNonNull(barista, "barista");
  }

  /**
   * The order form.
   */
  String form() {
    return render(
      "formPage.jte",
      Map.of(BARISTA, barista, "error", Optional.empty())
    );
  }

  /**
   * The order form again, explaining why the last order was refused.
   */
  String form(String error) {
    return render(
      "formPage.jte",
      Map.of(BARISTA, barista, "error", Optional.of(error))
    );
  }

  /**
   * The cup, the time it took to brew, and the order form to order another.
   */
  String result(Order order, Duration brewTime) {
    return render(
      "resultPage.jte",
      Map.of(BARISTA, barista, "order", order, "brewTime", brewTime)
    );
  }

  String notFound() {
    return render("notFoundPage.jte", Map.of(BARISTA, barista));
  }

  String methodNotAllowed() {
    return render("methodNotAllowedPage.jte", Map.of(BARISTA, barista));
  }

  private static String render(String template, Map<String, Object> params) {
    var output = new StringOutput();
    TEMPLATES.render(template, params, output);
    return output.toString();
  }
}
