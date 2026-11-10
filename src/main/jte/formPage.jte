@import java.util.Optional

@param Barista barista
@param Optional<String> error

@template.layout(barista = barista, content = @`
  @template.orderForm(heading = "What would you like?", error = error)
`)
