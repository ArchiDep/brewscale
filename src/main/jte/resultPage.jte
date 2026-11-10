@import java.time.Duration

@param Barista barista
@param Order order
@param Duration brewTime

@template.layout(barista = barista, queueScript = true, content = @`
  <section>
    <div class="latte" aria-hidden="true"></div>
    <h2>Here is your ${order.drink().label()}@if(order.name().isPresent()), ${order.name().get()}@endif!</h2>
    <p id="brewed" data-brew-seconds="${Seconds.format(brewTime, 3)}">Brewed in ${Seconds.format(brewTime, 1)}s by ${barista.name()}.</p>
    <p id="queue" hidden></p>
  </section>
  @template.orderForm(heading = "Another one?")
`)
