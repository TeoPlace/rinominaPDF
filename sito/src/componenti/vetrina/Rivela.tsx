import { useEffect, useRef, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

// ============================================================
//  Scroll-reveal: un blocco compare quando entra nello schermo.
//
//  Perche' un IntersectionObserver e non `animation-timeline: view()`:
//  la seconda e' piu' breve da scrivere ma non c'e' su Safari, e meta'
//  del traffico di un sito che vende software per Windows arriva
//  comunque da telefoni. Con l'observer il comportamento e' lo stesso
//  ovunque.
//
//  LA REGOLA UNICA DI QUESTO FILE: se c'e' un dubbio, si mostra.
//  Lo stato di partenza in CSS e' `opacity: 0`, quindi ogni caso non
//  previsto qui dentro non e' "un'animazione che non parte": e' una
//  PAGINA BIANCA. Su un sito che vende, e' il peggior guasto possibile,
//  e per giunta invisibile a chi lo pubblica — la pagina si apre bene
//  sul computer di chi l'ha fatta.
//
//  I tre casi in cui non si aspetta nessuno, e si mostra e basta:
//
//  1. prefers-reduced-motion. Non e' una spunta di accessibilita': chi
//     lo attiva spesso lo fa per l'emicrania o la chinetosi, e una
//     pagina che gli si muove sotto e' una pagina che chiude. Letto
//     PRIMA del primo render, cosi' non c'e' nemmeno un fotogramma di
//     movimento.
//
//  2. LA SCHEDA E' IN SECONDO PIANO AL CARICAMENTO. E' il caso che ha
//     morso davvero, il 2026-08-28: con `document.visibilityState`
//     "hidden" il browser non compone i fotogrammi, quindi
//     l'IntersectionObserver non riporta NIENTE e i 32 blocchi della
//     pagina restavano tutti a opacita' zero. Succede a chi apre il
//     link con il tasto centrale o da "apri in una nuova scheda", e
//     succede ai crawler che generano l'anteprima. Se nessuno sta
//     guardando non c'e' nessuna animazione da salvare: si mostra
//     tutto e via.
//
//  3. Niente IntersectionObserver nel browser.
//
//  E anche quando l'observer c'e' e serve, il primo schermo non lo
//  aspetta: al montaggio si misura, e cio' che e' gia' dentro (o sopra)
//  la finestra compare subito. L'observer resta per il resto.
//
//  Si osserva UNA VOLTA SOLA. Dopo il primo ingresso l'observer si
//  stacca: se il blocco si rianimasse a ogni passaggio, scorrere in su
//  una pagina gia' letta la farebbe lampeggiare tutta.
// ============================================================

/** Vero quando non si deve animare affatto: si parte gia' visibili. */
function subitoVisibile(): boolean {
  if (typeof window === "undefined") {
    return true;
  }

  if (typeof window.matchMedia === "function"
      && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return true;
  }

  // La scheda non e' in primo piano: nessuno vedrebbe l'animazione, e
  // l'observer non riporterebbe niente finche' resta cosi'.
  if (typeof document !== "undefined" && document.visibilityState === "hidden") {
    return true;
  }

  return false;
}

interface PropsRivela {
  children: ReactNode;
  /** Ritardo in millisecondi: serve a scaglionare gli elementi di una griglia. */
  ritardo?: number;
  className?: string;
  /** Il tag da usare. Le sezioni vogliono <section>, non un <div> in piu'. */
  come?: "div" | "section" | "li" | "article";
}

export function Rivela({ children, ritardo = 0, className, come = "div" }: PropsRivela) {
  const riferimento = useRef<HTMLElement | null>(null);
  const [visibile, impostaVisibile] = useState(subitoVisibile);

  useEffect(
    function () {
      const nodo = riferimento.current;

      if (nodo === null || visibile) {
        return;
      }

      // `alRitorno` piu' sotto e' una dichiarazione di funzione, quindi viene
      // issata: TypeScript la considera chiamabile prima del controllo qui
      // sopra e dentro ci ridarebbe `nodo` come possibilmente null. Questa
      // const ha il tipo giusto per dichiarazione, non per flusso.
      const elemento: HTMLElement = nodo;

      // Gia' dentro la finestra: comparire con un'animazione che parte da un
      // observer significherebbe un fotogramma vuoto sul primo schermo.
      const posizione = elemento.getBoundingClientRect();

      if (posizione.top < window.innerHeight) {
        impostaVisibile(true);
        return;
      }

      // Browser senza IntersectionObserver: si mostra e basta. Un contenuto
      // invisibile e' un guasto, un contenuto senza animazione no.
      if (typeof IntersectionObserver !== "function") {
        impostaVisibile(true);
        return;
      }

      const osservatore = new IntersectionObserver(
        function (voci) {
          const voce = voci[0];
          if (voce !== undefined && voce.isIntersecting) {
            impostaVisibile(true);
            osservatore.disconnect();
          }
        },
        // Il margine negativo in basso fa partire l'animazione quando il blocco
        // e' entrato per un pezzo, non appena spunta il primo pixel: cosi' si
        // vede muovere qualcosa che si sta gia' guardando.
        { threshold: 0, rootMargin: "0px 0px -12% 0px" }
      );

      osservatore.observe(elemento);

      // Rete di sicurezza per il caso 2 che arriva DOPO il montaggio: la
      // scheda era in primo piano, e' passata dietro, e ora torna. Chrome in
      // genere riconsegna le osservazioni da solo, ma "in genere" su una
      // pagina che puo' restare bianca non basta.
      function alRitorno() {
        if (document.visibilityState === "visible") {
          const dove = elemento.getBoundingClientRect();
          if (dove.top < window.innerHeight && dove.bottom > 0) {
            impostaVisibile(true);
            osservatore.disconnect();
          }
        }
      }

      document.addEventListener("visibilitychange", alRitorno);

      return function () {
        osservatore.disconnect();
        document.removeEventListener("visibilitychange", alRitorno);
      };
    },
    [visibile]
  );

  const Tag = come as "div";

  return (
    <Tag
      ref={riferimento as React.Ref<HTMLDivElement>}
      data-rivela=""
      data-visibile={visibile ? "si" : "no"}
      style={ritardo > 0 ? { transitionDelay: `${ritardo}ms` } : undefined}
      className={cn(className)}
    >
      {children}
    </Tag>
  );
}
