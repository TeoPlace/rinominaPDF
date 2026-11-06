import type { ComponentType, SVGProps } from "react";
import {
  Layers,
  LayoutGrid,
  WandSparkles,
  MousePointerClick,
  SlidersHorizontal,
  Columns2,
  Trash2,
  Smartphone,
  FileClock,
  Compass,
  ShieldCheck,
  Hand,
  Recycle,
  Lock,
  Check,
} from "lucide-react";

// ============================================================
//  Da chiave a icona.
//
//  prodotto.ts e' un file di DATI: se importasse componenti React
//  smetterebbe di essere il file che si copia da un sito prodotto
//  all'altro cambiando solo le stringhe. Quindi li' si scrive una
//  chiave ("scudo"), e la traduzione in componente sta qui.
//
//  Le chiavi sono in italiano come tutto il resto del progetto. Una
//  chiave sconosciuta non e' un errore fatale: si ripiega su una
//  spunta, perche' una pagina senza un'icona si legge lo stesso,
//  mentre una pagina che non compila non si legge affatto.
//
//  ATTENZIONE ai nomi di lucide: la 1.x ha rinominato parecchie
//  icone (Wand2 -> WandSparkles, CheckCircle2 -> CircleCheck). Prima
//  di aggiungerne una, controllare che esista davvero:
//    grep 'declare const Nome:' node_modules/lucide-react/dist/lucide-react.d.ts
// ============================================================

type Icona = ComponentType<SVGProps<SVGSVGElement>>;

const ICONE: Record<string, Icona> = {
  livelli: Layers,
  mucchi: LayoutGrid,
  bacchetta: WandSparkles,
  puntatore: MousePointerClick,
  cursori: SlidersHorizontal,
  confronto: Columns2,
  cestino: Trash2,
  iphone: Smartphone,
  storico: FileClock,
  bussola: Compass,
  scudo: ShieldCheck,
  mano: Hand,
  riciclo: Recycle,
  lucchetto: Lock,
};

export function iconaDi(chiave: string): Icona {
  const trovata = ICONE[chiave];
  return trovata === undefined ? Check : trovata;
}
