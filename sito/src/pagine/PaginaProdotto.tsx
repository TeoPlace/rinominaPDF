import { useState, useEffect } from "react";
import {
  Download,
  Check,
  Monitor,
  ArrowRight,
  ArrowDown,
  ShieldAlert,
  ChevronDown,
  Sparkles,
} from "lucide-react";
import { Button } from "@/componenti/ui/button";
import { Rivela } from "@/componenti/vetrina/Rivela";
import { Faq } from "@/componenti/vetrina/Faq";
import { iconaDi } from "@/componenti/vetrina/icone";
import { PRODOTTO } from "@/prodotto";
import * as servizioDownload from "@/servizi/servizioDownload";
import { VersioneInstaller } from "@/servizi/servizioDownload";
import { cn } from "@/lib/utils";

// ============================================================
//  Sito di un programma di lavoro (non in vendita): stessa pagina e stessi componenti dei siti
//  prodotto di teo.place, senza la parte commerciale — niente piani, niente registrazione,
//  niente account. Resta l'ordine che conta: gancio -> come funziona -> cosa fa -> limiti ->
//  domande -> di nuovo il bottone. Il bottone scarica teoSetup preselezionato sul prodotto
//  (blocco `wizard` del manifest), come in tutti gli altri.
// ============================================================

interface PropsDownload {
  versione: VersioneInstaller;
}

function BottoneDownload({ versione }: PropsDownload) {
  const indirizzo = servizioDownload.indirizzoInstaller(versione);

  if (indirizzo === null) {
    return (
      <Button size="lg" disabled className="h-11 gap-2 px-6 text-base">
        <Download className="size-4" />
        Presto disponibile
      </Button>
    );
  }

  return (
    <Button
      size="lg"
      render={<a href={indirizzo} download />}
      className="h-11 gap-2 px-6 text-base shadow-lg shadow-primary/20"
    >
      <Download className="size-4" />
      Scarica per Windows
    </Button>
  );
}

// Gli installer non sono firmati: si dice prima, con le parole che Windows userà, e si dà
// l'impronta del file che il bottone scarica davvero.
function NotaSicurezza({ versione }: PropsDownload) {
  if (versione.disponibile !== true) {
    return null;
  }

  const scaricato = servizioDownload.fileScaricato(versione);
  const impronta = scaricato === null ? null : scaricato.sha256;

  return (
    <details className="group mx-auto mt-6 max-w-xl overflow-hidden rounded-lg border border-border/60 bg-muted/20 text-left">
      <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-2.5 text-xs text-muted-foreground transition-colors hover:text-foreground focus-visible:text-foreground focus-visible:outline-none [&::-webkit-details-marker]:hidden">
        <ShieldAlert className="size-3.5 shrink-0" />
        Windows mostrerà un avviso: perché, e come proseguire
        <ChevronDown className="ms-auto size-3.5 shrink-0 transition-transform duration-200 group-open:rotate-180" />
      </summary>

      <div className="px-4 pb-4 text-sm">
        <p>
          L&apos;installer non è ancora firmato digitalmente, quindi compare{" "}
          &laquo;PC protetto da Windows&raquo;. Per proseguire: <em>Ulteriori informazioni</em>{" "}
          &rarr; <em>Esegui comunque</em>.
        </p>
        {impronta !== null && impronta.length > 0 && (
          <p className="mt-3 break-all text-xs text-muted-foreground">
            <span className="font-medium">SHA-256:</span> <code>{impronta}</code>
            <br />
            Per verificare: <code>Get-FileHash &lt;file&gt; -Algorithm SHA256</code>
          </p>
        )}
      </div>
    </details>
  );
}

function TitoloSezione({
  occhiello,
  titolo,
  sottotitolo,
}: {
  occhiello: string;
  titolo: string;
  sottotitolo?: string;
}) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">{occhiello}</p>
      <h2 className="mt-3 text-balance text-2xl font-bold tracking-tight sm:text-3xl">{titolo}</h2>
      {sottotitolo !== undefined && (
        <p className="mt-3 text-pretty text-muted-foreground">{sottotitolo}</p>
      )}
    </div>
  );
}

const NAVIGAZIONE = [
  { href: "#come", testo: "Come funziona" },
  { href: "#funzioni", testo: "Funzioni" },
  { href: "#limiti", testo: "Limiti" },
  { href: "#domande", testo: "Domande" },
];

export function PaginaProdotto() {
  const [versione, impostaVersione] = useState<VersioneInstaller>({
    disponibile: false,
    versione: null,
    file: null,
    dimensione: null,
    data: null,
  });
  const [scorso, impostaScorso] = useState(false);

  useEffect(function () {
    async function caricaInstaller() {
      const trovata = await servizioDownload.caricaVersione();
      impostaVersione(trovata);
    }

    caricaInstaller();
  }, []);

  useEffect(function () {
    function alloScorrimento() {
      impostaScorso(window.scrollY > 420);
    }

    alloScorrimento();
    window.addEventListener("scroll", alloScorrimento, { passive: true });

    return function () {
      window.removeEventListener("scroll", alloScorrimento);
    };
  }, []);

  let rigaVersione = `${PRODOTTO.requisiti} · installer non ancora pubblicato`;

  if (versione.disponibile === true) {
    const scaricato = servizioDownload.fileScaricato(versione);
    const quanto = scaricato === null ? versione.dimensione : scaricato.dimensione;
    rigaVersione = `Versione ${versione.versione} · ${quanto} · ${PRODOTTO.requisiti}`;
  }

  const indirizzoDownload = servizioDownload.indirizzoInstaller(versione);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header
        className={cn(
          "sticky top-0 z-40 border-b transition-all duration-300",
          scorso
            ? "border-border bg-background/90 backdrop-blur-md"
            : "border-transparent bg-background/60 backdrop-blur"
        )}
      >
        <div
          className={cn(
            "mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 transition-all duration-300",
            scorso ? "h-14" : "h-16"
          )}
        >
          <a href="#top" className="flex shrink-0 items-center gap-2">
            <span className="size-3 shrink-0 rounded-full bg-primary" />
            <span className="text-lg font-bold tracking-tight">{PRODOTTO.nome}</span>
          </a>

          <nav className="hidden items-center gap-1 text-sm text-muted-foreground lg:flex">
            {NAVIGAZIONE.map(function (voce) {
              return (
                <a
                  key={voce.href}
                  href={voce.href}
                  className="rounded-md px-2.5 py-1.5 transition-colors hover:bg-muted hover:text-foreground"
                >
                  {voce.testo}
                </a>
              );
            })}
          </nav>

          <div className="flex shrink-0 items-center gap-1">
            {indirizzoDownload !== null && (
              <Button
                size="sm"
                render={<a href={indirizzoDownload} download />}
                className={cn(
                  "gap-1.5 whitespace-nowrap transition-all duration-300",
                  scorso ? "opacity-100" : "pointer-events-none opacity-0"
                )}
              >
                <Download className="size-3.5" />
                Scarica
              </Button>
            )}
          </div>
        </div>
      </header>

      <main id="top">
        <section className="relative overflow-hidden px-4 pb-16 pt-14 sm:pt-20">
          <div aria-hidden="true" className="sfondo-hero" />

          <div className="relative mx-auto max-w-3xl text-center">
            <Rivela>
              <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/70 px-3 py-1 text-xs font-medium text-muted-foreground backdrop-blur">
                <Sparkles className="size-3.5 text-primary" />
                {PRODOTTO.tagline}
              </span>

              <h1 className="mt-6 text-balance text-4xl font-bold tracking-tight sm:text-6xl">
                {PRODOTTO.gancio}
              </h1>

              <p className="mx-auto mt-5 max-w-2xl text-pretty text-lg text-muted-foreground">
                {PRODOTTO.promessa}
              </p>
            </Rivela>

            <Rivela ritardo={120}>
              <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
                <BottoneDownload versione={versione} />
                <Button
                  size="lg"
                  variant="outline"
                  render={<a href="#come" />}
                  className="h-11 gap-2 px-6 text-base"
                >
                  Come funziona
                  <ArrowDown className="size-4" />
                </Button>
              </div>

              <p className="mt-4 text-sm text-muted-foreground">{PRODOTTO.prova}</p>

              <ul className="mt-7 flex flex-wrap items-center justify-center gap-x-5 gap-y-2 text-sm text-muted-foreground">
                {PRODOTTO.rassicurazioni.map(function (voce) {
                  return (
                    <li key={voce} className="flex items-center gap-1.5">
                      <Check className="size-4 shrink-0 text-primary" />
                      {voce}
                    </li>
                  );
                })}
              </ul>

              <p className="mt-6 flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
                <Monitor className="size-3.5 shrink-0" />
                {rigaVersione}
              </p>

              <NotaSicurezza versione={versione} />
            </Rivela>
          </div>
        </section>

        <section id="come" className="scroll-mt-20 border-t border-border/60 px-4 py-20">
          <Rivela>
            <TitoloSezione
              occhiello="Come funziona"
              titolo={PRODOTTO.titoloPassi}
              sottotitolo={PRODOTTO.sottotitoloPassi}
            />
          </Rivela>

          <ol className="mx-auto mt-12 grid max-w-5xl gap-6 md:grid-cols-3">
            {PRODOTTO.passi.map(function (passo, indice) {
              return (
                <Rivela
                  key={passo.titolo}
                  come="li"
                  ritardo={indice * 110}
                  className="relative flex flex-col gap-3 rounded-xl border border-border bg-card p-6"
                >
                  {indice < PRODOTTO.passi.length - 1 && (
                    <ArrowRight
                      aria-hidden="true"
                      className="absolute -right-5 top-1/2 hidden size-5 -translate-y-1/2 text-border md:block"
                    />
                  )}

                  <span className="text-sm font-semibold tabular-nums text-primary">
                    Passo {indice + 1}
                  </span>
                  <h3 className="text-lg font-semibold tracking-tight">{passo.titolo}</h3>
                  <p className="text-pretty text-sm leading-relaxed text-muted-foreground">
                    {passo.testo}
                  </p>
                </Rivela>
              );
            })}
          </ol>
        </section>

        <section id="funzioni" className="scroll-mt-20 border-t border-border/60 px-4 py-20">
          <Rivela>
            <TitoloSezione
              occhiello="Cosa fa"
              titolo={PRODOTTO.titoloFunzioni}
              sottotitolo="Ogni riga qui sotto è una cosa che il programma fa davvero."
            />
          </Rivela>

          <ul className="mx-auto mt-12 grid max-w-5xl gap-4 sm:grid-cols-2">
            {PRODOTTO.funzioni.map(function (funzione, indice) {
              const Icona = iconaDi(funzione.icona);

              return (
                <Rivela
                  key={funzione.titolo}
                  come="li"
                  ritardo={(indice % 2) * 90}
                  className="group flex gap-4 rounded-xl border border-border bg-card p-5 transition-colors hover:border-primary/40"
                >
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-primary transition-colors group-hover:bg-primary/10">
                    <Icona className="size-4" />
                  </span>
                  <div>
                    <h3 className="font-semibold tracking-tight">{funzione.titolo}</h3>
                    <p className="mt-1.5 text-pretty text-sm leading-relaxed text-muted-foreground">
                      {funzione.testo}
                    </p>
                  </div>
                </Rivela>
              );
            })}
          </ul>
        </section>

        <section
          id="limiti"
          className="scroll-mt-20 border-y border-border/60 bg-muted/20 px-4 py-20"
        >
          <Rivela>
            <TitoloSezione
              occhiello="Limiti"
              titolo="Cosa non fa, e cosa non è stato provato"
              sottotitolo="Quello che il programma non fa si scrive, non si tace."
            />
          </Rivela>

          <ul className="mx-auto mt-12 grid max-w-5xl gap-4 sm:grid-cols-2">
            {PRODOTTO.limiti.map(function (limite, indice) {
              const Icona = iconaDi(limite.icona);

              return (
                <Rivela
                  key={limite.titolo}
                  come="li"
                  ritardo={(indice % 2) * 90}
                  className="flex gap-4 rounded-xl border border-border bg-card p-6"
                >
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icona className="size-5" />
                  </span>
                  <div>
                    <h3 className="font-semibold tracking-tight">{limite.titolo}</h3>
                    <p className="mt-1.5 text-pretty text-sm leading-relaxed text-muted-foreground">
                      {limite.testo}
                    </p>
                  </div>
                </Rivela>
              );
            })}
          </ul>
        </section>

        <section id="domande" className="scroll-mt-20 px-4 py-20">
          <Rivela>
            <TitoloSezione
              occhiello="Domande"
              titolo="Quello che ci si chiede prima di scaricare"
            />
          </Rivela>

          <Rivela className="mx-auto mt-12 max-w-3xl">
            <Faq domande={PRODOTTO.faq} />
          </Rivela>
        </section>

        <section className="px-4 pb-24">
          <Rivela className="relative mx-auto max-w-4xl overflow-hidden rounded-2xl border border-border bg-gradient-to-b from-card to-muted/30 px-6 py-14 text-center">
            <div aria-hidden="true" className="sfondo-chiusura" />
            <div className="relative">
              <h2 className="text-balance text-2xl font-bold tracking-tight sm:text-3xl">
                {PRODOTTO.chiusura}
              </h2>
              <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
                <BottoneDownload versione={versione} />
              </div>
              <p className="mt-4 text-xs text-muted-foreground">{PRODOTTO.requisiti}</p>
            </div>
          </Rivela>
        </section>
      </main>

      <footer className="border-t py-10">
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 text-sm text-muted-foreground sm:flex-row sm:justify-between">
          <div className="flex items-center gap-2">
            <span className="size-2.5 shrink-0 rounded-full bg-primary" />
            <span className="font-semibold text-foreground">{PRODOTTO.nome}</span>
            <span className="hidden sm:inline">· {PRODOTTO.tagline}</span>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-x-5 gap-y-2">
            <a href="#domande" className="hover:text-foreground hover:underline">
              Domande
            </a>
            <a href="https://teo.place/" className="hover:text-foreground hover:underline">
              Le altre app di teo.place
            </a>
            <span>© {new Date().getFullYear()} teo.place</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
