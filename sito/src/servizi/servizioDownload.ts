import axios from "axios";

// ============================================================
//  Installer MSI: metadati letti da /scarica/versione.json.
//
//  Il manifest sta su disco accanto al .msi (/var/www/scarica/<slug>/)
//  e NON dentro la dist: cosi' pubblicare una versione nuova e'
//  copiare due file, senza ricompilare il sito.
//
//  Finche' `disponibile` e' false il bottone resta spento: meglio
//  un bottone disabilitato che un link a un 404.
// ============================================================

export interface InstallerWizard {
  file: string;
  versione: string | null;
  dimensione: string | null;
  data: string | null;
  sha256: string | null;
}

export interface VersioneInstaller {
  // Impronta del file, per chi vuole verificare da se' cosa ha scaricato. Facoltativa:
  // i manifest scritti prima che esistesse restano validi.
  sha256?: string | null;
  disponibile: boolean;
  versione: string | null;
  file: string | null;
  dimensione: string | null;
  data: string | null;
  // Il wizard teo.place preselezionato su questo prodotto: da qui in avanti e' LUI il file che si
  // scarica, e l'MSI resta sul server perche' e' il wizard a prenderlo. Facoltativo: un manifest
  // che non lo ha resta valido e il bottone torna a puntare all'MSI, com'era prima.
  wizard?: InstallerWizard | null;
}

const NON_DISPONIBILE: VersioneInstaller = {
  disponibile: false,
  versione: null,
  file: null,
  dimensione: null,
  data: null,
};

export async function caricaVersione(): Promise<VersioneInstaller> {
  try {
    const risposta = await axios.get<VersioneInstaller>("/scarica/versione.json");
    return risposta.data;
  } catch (errore) {
    // Manifest assente o illeggibile: il sito resta in piedi e mostra
    // semplicemente il download come non ancora disponibile.
    return NON_DISPONIBILE;
  }
}

export interface FileScaricato {
  nome: string;
  dimensione: string | null;
  sha256: string | null;
}

/**
 * Cosa scarica DAVVERO il bottone principale.
 *
 * Il wizard se il manifest lo pubblica, l'MSI altrimenti. Sta in una funzione sola perche' il
 * nome, la dimensione e l'impronta mostrata devono descrivere lo STESSO file: ricavarli da tre
 * punti diversi e' come si finisce a far verificare a qualcuno un'impronta che non gli tornera'
 * mai, e a dargli la colpa del download.
 */
export function fileScaricato(versione: VersioneInstaller): FileScaricato | null {
  if (versione.disponibile !== true) {
    return null;
  }

  const wizard = versione.wizard;

  if (wizard !== undefined && wizard !== null
      && typeof wizard.file === "string" && wizard.file.length > 0) {
    return { nome: wizard.file, dimensione: wizard.dimensione, sha256: wizard.sha256 };
  }

  if (versione.file === null) {
    return null;
  }

  return {
    nome: versione.file,
    dimensione: versione.dimensione,
    sha256: typeof versione.sha256 === "string" ? versione.sha256 : null,
  };
}

export function indirizzoInstaller(versione: VersioneInstaller): string | null {
  const scaricato = fileScaricato(versione);

  if (scaricato === null) {
    return null;
  }

  return `/scarica/${scaricato.nome}`;
}
