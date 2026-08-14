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

#instruzioni

	- su sorter-fx ci sono le instruzioni di come usare wsl per ricavare barcode, usa lo stesso metodo
	- su sorter-fx ci sta una gestione delle risorce del computer per controllo multi-thread, usa lo stesso metodo 
	- su sorter-fx ci sta il log, inspirati su quello ma fai a modo tuo