import { ChevronDown } from "lucide-react";
import type { Domanda } from "@/prodotto";

// ============================================================
//  Le domande, su <details>/<summary>.
//
//  Non un accordion scritto a mano con useState, e non per pigrizia:
//  <details> lo sa gia' fare il browser, quindi funziona con la
//  tastiera, lo legge lo screen reader senza aria-* da azzeccare, e
//  soprattutto Ctrl+F trova anche il testo delle risposte chiuse —
//  che in un accordion in JavaScript e' quasi sempre rotto. Chi cerca
//  "HEIC" nella pagina deve trovarlo.
//
//  APERTA LA PRIMA. La domanda numero uno di chi valuta un programma
//  che cancella foto e' "rischio di perdere qualcosa?", e lasciarla
//  chiusa vuol dire lasciare chiuso l'unico dubbio che tiene lontano
//  chi comprerebbe. Le altre restano chiuse: una lista di titoli si
//  scorre, un muro di testo no.
// ============================================================

interface PropsFaq {
  domande: Domanda[];
}

export function Faq({ domande }: PropsFaq) {
  return (
    <div className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-card">
      {domande.map(function (voce, indice) {
        return (
          <details key={voce.domanda} className="group" open={indice === 0}>
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 text-left text-sm font-medium transition-colors hover:bg-muted/40 focus-visible:bg-muted/40 focus-visible:outline-none [&::-webkit-details-marker]:hidden">
              {voce.domanda}
              <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-open:rotate-180" />
            </summary>
            <p className="px-5 pb-5 text-sm leading-relaxed text-muted-foreground">
              {voce.risposta}
            </p>
          </details>
        );
      })}
    </div>
  );
}
