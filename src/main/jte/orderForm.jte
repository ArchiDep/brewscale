@import java.util.Optional

@param String heading
<%-- Fully qualified: jte copies a default value into the templates that use it. --%>
@param Optional<String> error = java.util.Optional.empty()

<section>
  <h2>${heading}</h2>
  @if(error.isPresent())
    <p class="error">${error.get()}</p>
  @endif
  <form method="post" action="/orders">
    <label for="drink">Drink</label>
    <select id="drink" name="drink">
      @for(var drink : Drink.values())
        <option value="${drink.value()}">${drink.menuLabel()}</option>
      @endfor
    </select>
    <label for="name">Name for the cup <span class="optional">(optional)</span></label>
    <input id="name" name="name" maxlength="${Order.MAX_NAME_LENGTH}" autocomplete="given-name">
    <button>Order</button>
  </form>
  <%-- Once the order is sent, the form cannot be changed and shows that the
  order is brewing. It is made inert rather than disabled: the browser does not
  send disabled fields. A page restored with the Back button gets its form
  back. --%>
  <script>
    @raw
    {
      const form = document.currentScript.previousElementSibling;
      const button = form.querySelector('button');
      const label = button.textContent;
      form.addEventListener('submit', () => {
        form.inert = true;
        form.classList.add('brewing');
        button.textContent = 'Brewing…';
      });
      window.addEventListener('pageshow', event => {
        if (event.persisted) {
          form.inert = false;
          form.classList.remove('brewing');
          button.textContent = label;
        }
      });
    }
    @endraw
  </script>
</section>
