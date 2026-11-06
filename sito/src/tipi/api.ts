// Forme fisse delle risposte API (vedi API.md alla radice del progetto).

export interface ListaPaginata<T> {
  dati: T[];
  totale: number;
  pagina: number;
  perPagina: number;
}

export interface RispostaErrore {
  errore: string;
}

// Un filtro applicato, mostrato come chip nella schermata_crud.
export interface FiltroApplicato {
  chiave: string;
  etichetta: string;
  valore: string;
  diDefault: boolean;
}
