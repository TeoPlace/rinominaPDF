// ============================================================
//  Configurazione del prodotto: L'UNICO file che cambia fra i siti degli strumenti di lavoro
//  (copia del sito di SorterNext, e per la parte che si ritrova identica di tutti i siti
//  prodotto di teo.place).
//
//  TUTTO QUI DENTRO E' VERO, E LA FONTE E' IL PRODOTTO: il CLAUDE.md del repo e il codice. Cio'
//  che il programma non fa o non e' stato provato si scrive nei `limiti`, non si tace.
// ============================================================

export interface Passo {
  titolo: string;
  testo: string;
}

export interface Funzione {
  /** Chiave dell'icona, risolta in componenti/vetrina/icone.tsx */
  icona: string;
  titolo: string;
  testo: string;
}

export interface Domanda {
  domanda: string;
  risposta: string;
}

export interface ConfigurazioneProdotto {
  codice: string;
  nome: string;
  tagline: string;
  gancio: string;
  promessa: string;
  rassicurazioni: string[];
  prova: string;
  titoloPassi: string;
  sottotitoloPassi: string;
  passi: Passo[];
  titoloFunzioni: string;
  funzioni: Funzione[];
  limiti: Funzione[];
  faq: Domanda[];
  chiusura: string;
  requisiti: string;
}

export const PRODOTTO: ConfigurazioneProdotto = {
  "codice": "rinominapdf",
  "nome": "rinominaPDF",
  "tagline": "I PDF della cartella, rinominati con il loro barcode",
  "gancio": "Una cartella di PDF scansionati: chi è chi?",
  "promessa": "rinominaPDF apre ogni PDF di una cartella, ne legge il barcode e lo copia nella cartella di destinazione con il nome dello smistamento: barcode, esito, istat, anno e pacco. Gli originali restano dove sono.",
  "rassicurazioni": [
    "Gli originali non si toccano: si copia",
    "Ogni PDF non letto finisce in un elenco",
    "Si aggiorna da solo"
  ],
  "prova": "Si installa con teoSetup, un clic. È un programma di lavoro: niente account, niente licenza.",
  "titoloPassi": "Dalla cartella ai PDF rinominati",
  "sottotitoloPassi": "Una schermata per i dati, una per guardarlo lavorare.",
  "passi": [
    {
      "titolo": "Scegli le cartelle",
      "testo": "La cartella con i PDF da leggere e quella dove scrivere le copie rinominate."
    },
    {
      "titolo": "Dai i dati del lotto",
      "testo": "L’esito da un elenco (COMPIUTAGIACENZA, CONSEGNATO, RIFIUTATO, TRASFERITO, IRREPERIBILE, EMESSOCAD, DECEDUTO, ATTIVITACESSATA), l’istat, completato con gli zeri a sinistra fino a nove cifre, l’anno, controllato, e il pacco."
    },
    {
      "titolo": "Avvia",
      "testo": "Ogni PDF diventa <barcode>-sorter-<esito>-<istat>-<anno>-<pacco>.pdf, per esempio RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf. Una barra dice a che punto è, un pannello mostra memoria, processore e lavoratori."
    }
  ],
  "titoloFunzioni": "Fatto per cartelle grosse",
  "funzioni": [
    {
      "icona": "bacchetta",
      "titolo": "Quattro famiglie di codici",
      "testo": "Riconosce le stesse di SorterNext: due lettere e dieci cifre (RB0002858945), dodici cifre che iniziano per 2, 3, 6 o 7, ABW o ACW con otto cifre, diciassette cifre anche precedute da ARG. Se in un PDF ce n’è più d’uno, vince il primo tipo dell’elenco."
    },
    {
      "icona": "storico",
      "titolo": "Tre elenchi nella cartella di destinazione",
      "testo": "LOG_OK.txt con il nome vecchio e quello nuovo, LOG_DATAMATRIX_NON_LETTI.txt con i PDF di cui non ha trovato il codice e cosa ci ha letto, LOG_ERRORI.txt con quelli che non è riuscito ad aprire."
    },
    {
      "icona": "cursori",
      "titolo": "Si adatta al computer",
      "testo": "Parte con un lavoratore e ne aggiunge o ne toglie guardando processore e disco, come faceva sorter-fx."
    },
    {
      "icona": "mucchi",
      "titolo": "Nessun file sovrascritto",
      "testo": "Se due PDF portano allo stesso nome, il secondo prende _1, il terzo _2 e così via."
    },
    {
      "icona": "riciclo",
      "titolo": "Si aggiorna da solo",
      "testo": "Controlla all’avvio, poi ogni sei ore e quando premi «Controlla aggiornamenti»: scarica la versione nuova, ne verifica l’impronta, si chiude, la installa e si riapre."
    },
    {
      "icona": "scudo",
      "titolo": "Chiudere non rovina niente",
      "testo": "Se lo chiudi mentre lavora, ferma la coda e aspetta, fino a 90 secondi, che i file già presi siano copiati per intero, invece di lasciare a metà un PDF con il nome giusto."
    }
  ],
  "limiti": [
    {
      "icona": "lucchetto",
      "titolo": "Scansioni fax degradate",
      "testo": "Su 30 PDF di scansioni a 1 bit, compresse come fax e a bassa risoluzione, il lettore incluso ha trovato il codice giusto in 4. Quei PDF finiscono negli elenchi dei non letti, non con un nome sbagliato."
    },
    {
      "icona": "mano",
      "titolo": "Lento senza WSL",
      "testo": "Il lettore incluso (ZXing) impiega da 20 a 75 secondi per PDF su quelle scansioni. Se sul PC c’è WSL con Scandit, come per sorter-fx, usa quello."
    },
    {
      "icona": "confronto",
      "titolo": "Il primo codice valido decide",
      "testo": "Non confronta fronte e retro e non chiede all’operatore: se un PDF contiene due codici della stessa famiglia, prende il primo che ha letto."
    }
  ],
  "faq": [
    {
      "domanda": "Modifica i PDF originali?",
      "risposta": "No. Li copia nella cartella di destinazione con il nome nuovo; la cartella di partenza resta com’era."
    },
    {
      "domanda": "Cosa succede ai PDF che non riesce a leggere?",
      "risposta": "Non vengono copiati e finiscono in LOG_DATAMATRIX_NON_LETTI.txt, nella cartella di destinazione, con i codici che ha trovato ma non ha riconosciuto."
    },
    {
      "domanda": "Serve WSL o Scandit?",
      "risposta": "No. Se ci sono li usa, come sorter-fx; altrimenti legge i barcode da solo, più lentamente."
    },
    {
      "domanda": "Come lo installo e lo aggiorno?",
      "risposta": "Con teoSetup: il bottone di questa pagina scarica il wizard già puntato su rinominaPDF. Gli aggiornamenti dopo li propone il programma stesso."
    },
    {
      "domanda": "Perché Windows dice che l’app non è sicura?",
      "risposta": "Perché l’installer non è ancora firmato digitalmente, e Windows avvisa su tutto ciò che non lo è. Compare «PC protetto da Windows»: Ulteriori informazioni, poi Esegui comunque. Sotto al bottone c’è l’impronta SHA-256 del file per verificarlo."
    }
  ],
  "chiusura": "Mettilo davanti a una cartella di PDF",
  "requisiti": "Windows 10 o superiore (64 bit) — Java è incluso, non serve installare altro"
};
