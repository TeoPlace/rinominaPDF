# CLAUDE.md — rinominaPDF

**Prima leggi `STANDARD.md`**: toolchain, versionamento, nomenclatura degli artefatti, metodo di
packaging (fat-jar + jpackage, con la JVM dentro: l'utente finale non installa ne' Java ne' JavaFX)
e convenzioni JavaFX valgono per tutta la cartella e non si ripetono qui.

#workspace e dont-touch rule

	- usa C:\Users\tsousasilva\Desktop\projects\sorter-fx\2.0.4 solo come riferimento ###non fare modifiche su questa cartella### mi riferirà a questa cartella come sorter-fx
	- usa C:\Users\tsousasilva\Desktop\projects\queryF5  solo come riferimento ###non fare modifiche su questa cartella###  mi riferirà a questa cartella come queryF5
	- usa C:\Users\tsousasilva\Desktop\TEST PDF\pacchi_iniziale\063049_NAPOLI\2026\MP297\INTEGRAA-CONSEGNATO come test-case di entrata  ###non fare modifiche su questa cartella###  mi riferirà a questa cartella come case-input
	- usa C:\Users\tsousasilva\Desktop\TEST PDF\pacchi_iniziale\OUTPUT come obietivo da raggiungere ###non fare modifiche su questa cartella###  mi riferirà a questa cartella come case-output
	- usa C:\Users\tsousasilva\Desktop\projects\rinominaPDF come workspace ###sei libero di fare modifiche su questa cartella###  mi riferirà a questa cartella come workspace

#il progetto

	rinominaPDF ha l'obbietivo di :
		-aprire un pdf
		-leggere barcodes nel file pdf 
		-selezionare il barcode giusto 
		-rinominare pdf con i dati:
			RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf
			<barcode> - sorter - <status> - <istat> - <anno> - <pacco> .pdf dove :
				-barcode: trovato all'interno del pdf e selezionato quello giusto 
				-sorter: testo fisso 
				-status: selezionato dal'utente tramite UI dalle opzioni:
					COMPIUTAGIACENZA
					CONSEGNATO
					RIFIUTATO
					TRASFERITO
					IRREPERIBILE
					EMESSOCAD
					DECEDUTO
					ATTIVITACESSATA

				-istat: inserito dall'utente tramite UI formato 000000000, quindi aggiungere 0 a sinistra 
				-anno: inserito dall'utente (controllo regex se non è anno non va avanti)
				-pacco: inserito dall'utente 
				
#flusso UI 
	- utente installa il msi, apri il file .exe 
	- si apri un'interfaccia che chiedi :
		-Input : folder dove sono i file .pdf 
		-Output: folder dove i file rinominati saranno salvati 
		-Esito: elenco dei esiti da selezionare 
		-Istat: input 
		-Anno: input 
		-pacco: input 
	-carrica view con: 
		-progress-bar 0-100 che si aggiorna
		-tasto per aprire/chiudere log-panel 
		-log-panel con le risorce utilizate (memoria, processamento) e stato delle thread 
		-animazione leggera che rapresenta la rinomina dei file stile gioco anni 90

#lettura barcode (dalla 1.0.4)

	- Un solo lettore: `sorter_barcode.dll` (libreria di sorter-barcode, release pubblica
	  lib_sorter_barcode_windows, copiata in `lib/sorter-barcode/` con LEGGIMI e sha256), chiamata con la
	  Foreign Function API da `core/SorterBarcode`. Scandit/WSL e ZXing tolti su richiesta dell'utente
	  (2026-11-13): niente lettore di riserva. Senza dll l'elaborazione si ferma con un errore fatale.
	  Misure in README, "Lettura barcode".
	- La dll e' a 64 bit, va accanto al jar (`build-msi.ps1` la copia in `jpackage-input`) e la JVM gira con
	  `--enable-native-access=ALL-UNNAMED` (java-options di jpackage, argLine di surefire).
	- La dll apre i file con `fopen` (codepage ANSI) e per un PDF senza immagini che sa aprire risponde -2:
	  in entrambi i casi `BarcodeReader.read` renderizza le pagine con PDFBox e le passa a `sb_read_gray`.
	  Su un PDF che apre ma senza codice valido NON si renderizza: a 300 DPI legge meno che dal file.
	- I PDF veri per le misure: `Desktop\bench-250gb` (66.918 PDF gia' rinominati, il barcode atteso e' nel
	  nome). Il case-input e il case-output qui sopra non esistono piu' (verificato il 2026-11-12).
	- Difetto noto, non del lettore: la ricerca come sottostringa (`INNER_CODES`) prende `OO6986475960` da
	  `...OOOO698647596022` nel DataMatrix di Poste: 3 nomi sbagliati su 300 con la sola libreria.

#instruzioni

	- ~~su sorter-fx ci sono le instruzioni di come usare wsl per ricavare barcode, usa lo stesso metodo~~
	  superata dalla 1.0.4: si legge solo con sorter_barcode, niente WSL (vedi "lettura barcode")
	- su sorter-fx ci sta una gestione delle risorce del computer per controllo multi-thread, usa lo stesso metodo 
	- su sorter-fx ci sta il log, inspirati su quello ma fai a modo tuo

#la finestra (dalla 1.0.3)

	- Le cinque zone di STANDARD.md, su AtlantaFX PrimerDark fisso con l'accento verde del prodotto:
	  `ui/Shell` (titolo, barra di sinistra, piede, flusher unico a 300 ms), `ui/Registro` (a destra),
	  al centro `SetupPane`, `ProcessPane`, `RiepilogoPane`, `ImpostazioniPane`.
	- `ui/AppSession` tiene job, storico e righe. Lo storico e' in `%LOCALAPPDATA%\rinominaPDF\elaborazioni.json`
	  (`core/StoricoStore`): una voce rimasta "in corso" viene da un programma morto a meta' e si
	  rilegge interrotta. La fine si registra sul thread del job, non su quello FX, che alla chiusura e' fermo.
	- Un job alla volta (`RenameJob.threadStatus` e `Globals` sono statici): il form si compila ma non parte.
	- La PRIMA `Pc.disk()` puo' durare quasi un minuto (55 s misurati): i sensori hanno un thread
	  loro, separato da quello dello storico, e mai il thread FX.
	- Scelte rispetto alla lista di controllo: niente licenza (strumento gratuito), niente i18n e
	  niente selettore di tema (solo italiano, un tema). "Impostazioni" non ha regolazioni finte.

#aggiornamento automatico

	- `core/update` + `ui/UpdateDialog` sono copiati da listBigStorage (vedi STANDARD.md della linea,
	  "Il controllo aggiornamenti dentro l'app"): controllo 5 s dopo l'avvio, poi ogni 6 ore, piu' il
	  pulsante "Controlla aggiornamenti" nella barra di sinistra. Legge `AppInfo.HOME_URL` + `scarica/versione.json`.
	- Pubblicazione su KVM2 (2026-10-30): `/var/www/scarica/rinominapdf/` con `rinominaPDF-1.0.1.msi`
	  e `versione.json` (sha256 compreso). Stesso giorno la 1.0.2 (famiglie di barcode di SorterNext,
	  vedi `Globals.BARCODE_FAMILIES`), manifest precedente in `versione.json.bak-1.0.1`. Vhost `/etc/nginx/sites-available/rinominapdf.teo.place` e record
	  DNS su Cloudflare attivi: manifest e MSI rispondono in HTTPS, sha256 verificato dal download.
	- Dal 2026-10-30 e' di nuovo nel wizard, **gratuito come SorterNext** (STANDARD.md, "Gli
	  strumenti di lavoro gratuiti"): niente licenza, niente authenticator, niente Lemon Squeezy.
	  Sito in `sito/` (copia di quello di SorterNext, cambia solo `src/prodotto.ts`, tema `foresta`,
	  icona `public/icona.png` presa da `icon.ico`), pubblicato in `/var/www/html/rinominapdf/dist/`;
	  vhost in `sito/rinominapdf.teo.place`. Bottone e scheda sulla home puntano a
	  `teoSetup-rinominapdf-ultimo.exe`.
	- La 1.0.3 (finestra a cinque zone) e' pubblicata dal 2026-11-06, manifest precedente in
	  `versione.json.bak-1.0.2`, blocco `wizard` conservato.
	- La 1.0.4 (solo sorter_barcode) e' pubblicata dal 2026-11-13: MSI e manifest in
	  `/var/www/scarica/rinominapdf/` (manifest precedente in `versione.json.bak-1.0.3`, blocco `wizard`
	  1.0.13 conservato), sito nuovo in `/var/www/html/rinominapdf/dist/` (il precedente in
	  `dist.bak-1.0.3`). Prima: installata qui sopra la 1.0.0, una sola voce in App installate.
	  Verificati da fuori manifest, download (sha256 uguale) e pagina.
	- L'esito dell'aggiornamento precedente, e "hai gia' l'ultima versione", vanno nel registro.
	- Chiudere l'app durante un'elaborazione la interrompe e aspetta (max 90 s) i file gia' presi:
	  i worker sono daemon e uccisi a meta' di `Files.copy` lascerebbero un PDF troncato col nome giusto.
