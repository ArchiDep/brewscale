@import gg.jte.Content

@param Barista barista
@param Content content
@param boolean queueScript = false

<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Brewscale</title>
    <style>
      @raw
      /*
       * Everything colourful is mixed from --barista, the barista's colour, so
       * that each barista's café has a look of its own.
       */
      body {
        --cream: #f6ead9;
        --cream-dim: rgb(246 234 217 / 68%);
        --roast: color-mix(in oklab, var(--barista) 9%, #150c07);
        --accent-light: color-mix(in oklab, var(--barista) 55%, #ffe8c7);
        --accent-glow: color-mix(in srgb, var(--barista) 55%, transparent);
        --accent-soft: color-mix(in srgb, var(--barista) 22%, transparent);
        --caramel: color-mix(in oklab, var(--barista) 40%, #c98a4b);
        --foam: color-mix(in oklab, var(--barista) 12%, #fbf1e4);
        --crema: color-mix(in oklab, var(--barista) 18%, #b9773f);

        color-scheme: dark;
        min-height: 100vh;
        margin: 0;
        overflow-x: hidden;
        background:
          radial-gradient(
            ellipse 80% 60% at 15% -10%,
            var(--accent-soft),
            transparent 70%
          ),
          radial-gradient(
            ellipse 70% 50% at 110% 110%,
            color-mix(in srgb, var(--caramel) 25%, transparent),
            transparent 70%
          ),
          var(--roast);
        color: var(--cream);
        font-family: system-ui, sans-serif;
        line-height: 1.5;
      }

      /* Blurred wisps of aroma drifting behind the glass. */
      .aroma {
        position: fixed;
        inset: 0;
        z-index: -1;
        overflow: hidden;
        pointer-events: none;
      }
      .aroma span {
        position: absolute;
        width: 38vmax;
        height: 38vmax;
        border-radius: 50%;
        filter: blur(90px);
        opacity: 0.4;
        animation: drift 22s ease-in-out infinite alternate;
      }
      .aroma span:nth-child(1) {
        top: -14vmax;
        left: -10vmax;
        background: var(--barista);
      }
      .aroma span:nth-child(2) {
        right: -12vmax;
        bottom: -18vmax;
        background: var(--caramel);
        animation-delay: -8s;
      }
      .aroma span:nth-child(3) {
        top: 45%;
        left: 50%;
        width: 22vmax;
        height: 22vmax;
        background: var(--accent-light);
        opacity: 0.18;
        animation-delay: -15s;
      }
      @keyframes drift {
        to {
          transform: translate(5vmax, 4vmax) scale(1.15);
        }
      }

      header {
        display: flex;
        flex-wrap: wrap;
        gap: 1rem 1.5rem;
        align-items: center;
        justify-content: space-between;
        max-width: 34rem;
        margin: 0 auto;
        padding: 2rem 1.5rem 0.5rem;
      }
      .brand {
        display: flex;
        gap: 0.85rem;
        align-items: center;
      }
      header h1 {
        margin: 0;
        background: linear-gradient(100deg, var(--cream), var(--accent-light));
        -webkit-background-clip: text;
        background-clip: text;
        color: transparent;
        font-family: 'Iowan Old Style', 'Palatino Linotype', Palatino, Georgia,
          serif;
        font-size: 2.1rem;
        font-style: italic;
        letter-spacing: -0.02em;
      }

      /* The logo: a cup in the barista's colour, with steam rising from it. */
      .logo {
        width: 3.25rem;
        height: 3.25rem;
        filter: drop-shadow(0 6px 14px var(--accent-glow));
      }
      .logo .cup {
        fill: var(--barista);
      }
      .logo .handle {
        fill: none;
        stroke: var(--barista);
        stroke-width: 4;
      }
      .logo .saucer {
        fill: var(--accent-light);
      }
      .logo .steam path {
        fill: none;
        stroke: var(--cream);
        stroke-linecap: round;
        stroke-width: 2.5;
        opacity: 0;
        animation: steam 3s ease-in-out infinite;
      }
      .logo .steam path:nth-child(2) {
        animation-delay: -1s;
      }
      .logo .steam path:nth-child(3) {
        animation-delay: -2s;
      }
      @keyframes steam {
        0% {
          opacity: 0;
          transform: translateY(5px);
        }
        40% {
          opacity: 0.85;
        }
        100% {
          opacity: 0;
          transform: translateY(-7px);
        }
      }

      /* The barista's badge, with a light that shows they are on duty. */
      header p {
        display: flex;
        gap: 0.55rem;
        align-items: center;
        margin: 0;
        padding: 0.4rem 1rem;
        border: 1px solid var(--accent-light);
        border-radius: 999px;
        background: var(--barista);
        box-shadow:
          0 0 0 4px var(--accent-soft),
          0 8px 28px var(--accent-glow);
        color: #fff;
        font-size: 0.9rem;
        font-weight: 600;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }
      header p::before {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: 50%;
        background: #fff;
        box-shadow: 0 0 8px #fff;
        content: '';
        animation: on-duty 2s ease-in-out infinite;
      }
      @keyframes on-duty {
        50% {
          opacity: 0.35;
        }
      }

      main {
        max-width: 34rem;
        margin: 1.5rem auto 3rem;
        padding: 0 1.5rem;
      }

      /* Frosted glass cards, edged with the barista's colour. */
      section {
        /* Contains the floated cup. */
        display: flow-root;
        position: relative;
        margin-bottom: 1.5rem;
        padding: 1.75rem;
        border: 1px solid color-mix(in srgb, var(--barista) 35%, rgb(255 255 255 / 12%));
        border-radius: 1.5rem;
        background: linear-gradient(
          160deg,
          rgb(255 248 240 / 9%),
          rgb(255 248 240 / 3%)
        );
        box-shadow:
          0 24px 60px rgb(0 0 0 / 45%),
          inset 0 1px 0 rgb(255 255 255 / 10%);
        backdrop-filter: blur(20px) saturate(140%);
        -webkit-backdrop-filter: blur(20px) saturate(140%);
        animation: serve 0.5s ease-out both;
      }
      section + section {
        animation-delay: 0.1s;
      }
      @keyframes serve {
        from {
          opacity: 0;
          transform: translateY(12px);
        }
      }
      h2 {
        margin: 0 0 0.5rem;
        font-family: 'Iowan Old Style', 'Palatino Linotype', Palatino, Georgia,
          serif;
        font-size: 1.6rem;
        font-weight: 600;
      }
      a {
        color: var(--accent-light);
        text-decoration-color: var(--accent-glow);
        text-underline-offset: 0.2em;
      }

      label {
        display: block;
        margin-top: 1.1rem;
        color: var(--cream-dim);
        font-size: 0.8rem;
        font-weight: 600;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }
      .optional {
        font-weight: normal;
        letter-spacing: 0;
        text-transform: none;
      }
      select,
      input {
        box-sizing: border-box;
        width: 100%;
        margin-top: 0.35rem;
        padding: 0.7rem 0.9rem;
        border: 1px solid rgb(255 255 255 / 14%);
        border-radius: 0.85rem;
        background: rgb(0 0 0 / 28%);
        color: var(--cream);
        font: inherit;
        transition:
          border-color 0.2s,
          box-shadow 0.2s;
      }
      select:focus,
      input:focus {
        border-color: var(--accent-light);
        box-shadow: 0 0 0 4px var(--accent-soft);
        outline: none;
      }
      option {
        background: var(--roast);
      }
      button {
        width: 100%;
        margin-top: 1.6rem;
        padding: 0.85rem 1.5rem;
        border: 0;
        border-radius: 0.85rem;
        background: linear-gradient(
          135deg,
          color-mix(in oklab, var(--barista) 82%, #fff),
          var(--barista) 50%,
          color-mix(in oklab, var(--barista) 75%, #000)
        );
        box-shadow:
          0 10px 30px var(--accent-glow),
          inset 0 1px 0 rgb(255 255 255 / 25%);
        color: #fff;
        cursor: pointer;
        font: inherit;
        font-size: 1.05rem;
        font-weight: 700;
        letter-spacing: 0.03em;
        transition:
          transform 0.15s,
          box-shadow 0.15s;
      }
      button:hover {
        box-shadow:
          0 14px 38px var(--accent-glow),
          inset 0 1px 0 rgb(255 255 255 / 25%);
        transform: translateY(-2px);
      }
      button:active {
        transform: translateY(1px);
      }

      /* While the order brews: faded fields and a spinning button. */
      .brewing label,
      .brewing select,
      .brewing input {
        opacity: 0.45;
      }
      .brewing button {
        display: flex;
        gap: 0.6rem;
        align-items: center;
        justify-content: center;
        cursor: progress;
        animation: pulse 0.9s ease-in-out infinite alternate;
      }
      .brewing button::before {
        width: 1rem;
        height: 1rem;
        border: 2px solid rgb(255 255 255 / 35%);
        border-top-color: #fff;
        border-radius: 50%;
        content: '';
        animation: spin 0.8s linear infinite;
      }
      @keyframes pulse {
        to {
          filter: brightness(1.2);
        }
      }
      @keyframes spin {
        to {
          transform: rotate(1turn);
        }
      }

      .error {
        padding: 0.65rem 0.9rem;
        border: 1px solid rgb(255 120 100 / 45%);
        border-radius: 0.85rem;
        background: rgb(255 90 70 / 14%);
        color: #ffb4a6;
        font-weight: 600;
      }

      /*
       * The served cup, seen from above: a tulip poured in foam tinted with
       * the barista's colour, in a porcelain rim.
       */
      .latte {
        float: right;
        width: 6.5rem;
        height: 6.5rem;
        margin: -0.25rem 0 0.75rem 1rem;
        border-radius: 50%;
        background:
          radial-gradient(
            circle closest-side,
            transparent 0 78%,
            #e9ddd0 80%,
            #fbf6f0 92% 100%
          ),
          radial-gradient(
            ellipse 15% 7% at 50% 33%,
            var(--foam) 0 85%,
            transparent 100%
          ),
          radial-gradient(
            ellipse 22% 8.5% at 50% 47%,
            var(--foam) 0 85%,
            transparent 100%
          ),
          radial-gradient(
            ellipse 28% 10% at 50% 62%,
            var(--foam) 0 85%,
            transparent 100%
          ),
          radial-gradient(
            ellipse 1.6% 28% at 50% 50%,
            var(--foam) 0 80%,
            transparent 100%
          ),
          radial-gradient(
            circle closest-side,
            var(--crema) 0 35%,
            #5a321a 78%
          );
        box-shadow:
          0 0 0 6px rgb(255 255 255 / 6%),
          0 12px 34px var(--accent-glow);
      }
      .latte::after {
        display: block;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background: conic-gradient(
          from 0deg,
          transparent,
          rgb(255 255 255 / 22%),
          transparent 30%
        );
        content: '';
        animation: swirl 6s linear infinite;
      }
      @keyframes swirl {
        to {
          transform: rotate(1turn);
        }
      }
      #brewed {
        display: inline-block;
        margin: 0.25rem 0 0;
        padding: 0.3rem 0.75rem;
        border-radius: 0.6rem;
        background: rgb(0 0 0 / 25%);
        color: var(--cream-dim);
        font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
        font-size: 0.9rem;
      }
      #queue {
        clear: both;
        margin: 1rem 0 0;
        padding: 0.75rem 1rem;
        border-left: 3px solid var(--accent-light);
        border-radius: 0 0.75rem 0.75rem 0;
        background: linear-gradient(90deg, var(--accent-soft), transparent);
        font-weight: 600;
      }

      @media (prefers-reduced-motion: reduce) {
        *,
        *::before,
        *::after {
          animation: none !important;
          transition: none !important;
        }
        .logo .steam path {
          opacity: 0.6;
        }
      }
      @endraw
    </style>
  </head>
  <body style="--barista: ${barista.colour()}">
    <div class="aroma" aria-hidden="true"><span></span><span></span><span></span></div>
    <header>
      <div class="brand">
        <svg class="logo" viewBox="0 0 64 64" aria-hidden="true">
          <g class="steam">
            <path d="M22 20c-4-4 4-7 0-12"/>
            <path d="M31 20c-4-4 4-7 0-12"/>
            <path d="M40 20c-4-4 4-7 0-12"/>
          </g>
          <path class="handle" d="M47 30h4a7 7 0 0 1 0 14h-5"/>
          <path class="cup" d="M10 25h38v12c0 9-7 16-16 16h-6c-9 0-16-7-16-16z"/>
          <rect class="saucer" x="5" y="55" width="48" height="4" rx="2"/>
        </svg>
        <h1>Brewscale</h1>
      </div>
      <p>${barista.title()}</p>
    </header>
    <main>
      ${content}
    </main>
    @if(queueScript)
      <%-- Shows how long the customer waited, which only the browser knows: the
      server is busy with the previous order while the next one waits, so it
      never sees when that one arrived. The sentence stays hidden in a browser
      that does not provide the navigation's timing. --%>
      <script>
        @raw
        const timing = performance.getEntriesByType('navigation')[0];
        if (timing && timing.responseStart > 0) {
          const waited = (timing.responseStart - timing.requestStart) / 1000;
          const brewed = Number(document.getElementById('brewed').dataset.brewSeconds);
          const queue = document.getElementById('queue');
          queue.textContent =
            waited - brewed < 0.05
              ? `You waited ${waited.toFixed(1)}s: there was no queue.`
              : `You waited ${waited.toFixed(1)}s: the rest of the time, you were in the queue.`;
          queue.hidden = false;
        }
        @endraw
      </script>
    @endif
  </body>
</html>
