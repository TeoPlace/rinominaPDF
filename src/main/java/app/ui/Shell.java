package app.ui;

import app.AppInfo;
import app.core.Elaborazione;
import app.core.JobParams;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * La finestra: le cinque zone dello standard (vedi STANDARD.md, "L'impianto della finestra").
 * Barra del titolo nostra, barra di sinistra con lo storico, centro, registro, piede. Lo stato
 * del lavoro sta in {@link AppSession}, qui solo cosa si sta guardando.
 *
 * <p>Niente controllo della licenza nella barra del titolo: rinominaPDF e' uno strumento gratuito.
 */
public final class Shell {

    private static final double LARGHEZZA_BARRA_APERTA = 240;
    private static final double LARGHEZZA_BARRA_CHIUSA = 52;
    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private final Stage stage;
    private final AppSession session;
    private final Path controlDir;
    private final Runnable controllaAggiornamenti;

    private final BorderPane sidebar = new BorderPane();
    private final VBox listaStorico = new VBox(2);
    private final ScrollPane centro = new ScrollPane();
    private final Registro registro;
    private final Label riepilogoPiede = new Label();
    private final SetupPane setup;
    private final Timeline flusher;

    private Button nuovaButton;
    private Button aggiornamentiButton;
    private Button impostazioniButton;
    private Button maxButton;

    private boolean barraAperta = true;
    private boolean inImpostazioni;
    private Long selezionata; // iniziataIl dell'elaborazione a schermo, null = form
    private ProcessPane processPane;

    private double dragDX;
    private double dragDY;
    private boolean maximized;
    private Rectangle2D savedBounds;

    public Shell(Stage stage, AppSession session, Path controlDir, Runnable controllaAggiornamenti) {
        this.stage = stage;
        this.session = session;
        this.controlDir = controlDir;
        this.controllaAggiornamenti = controllaAggiornamenti;
        this.registro = new Registro(session);
        this.setup = new SetupPane(this::avvia);

        centro.setFitToWidth(true);
        centro.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centro.getStyleClass().add("rp-scroll");

        BorderPane frame = new BorderPane();
        frame.getStyleClass().add("rp-window");
        frame.setTop(costruisciBarraTitolo());
        frame.setLeft(costruisciBarraSinistra());
        frame.setCenter(centro);
        frame.setRight(registro.node());
        frame.setBottom(costruisciPiede());

        Rectangle2D schermo = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(frame, Math.min(1200, schermo.getWidth()), Math.min(760, schermo.getHeight()));
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        // Non si vede, ma e' quello che Windows mostra nella barra delle applicazioni e in Alt+Tab.
        stage.setTitle(AppInfo.NAME + " " + AppInfo.VERSION);
        stage.getIcons().setAll(Dialogs.icone());
        stage.setMinWidth(980);
        stage.setMinHeight(600);
        WindowResizer.install(stage, scene);

        session.setSuFine(this::elaborazioneFinita);
        mostraForm();

        // Un solo flusher per tutta la finestra: le righe e i numeri arrivano a blocchi.
        flusher = new Timeline(new KeyFrame(Duration.millis(300), e -> aggiorna()));
        flusher.setCycleCount(Animation.INDEFINITE);
    }

    public void show() {
        aggiorna();
        flusher.play();
        stage.show();
    }

    public void ferma() {
        flusher.stop();
        if (processPane != null) processPane.fermaAnimazione();
    }

    public Stage stage() {
        return stage;
    }

    private void aggiorna() {
        registro.aggiorna();
        if (processPane != null) processPane.aggiorna();
        riepilogoPiede.setText(testoPiede());
    }

    private String testoPiede() {
        if (session.inCorso()) {
            return "In corso  ·  " + session.fatti() + " / " + session.totale() + " file  ·  Rinominati "
                    + session.rinominati() + "  ·  Non letti " + session.nonLetti() + "  ·  Errori " + session.errori();
        }
        List<Elaborazione> voci = session.storico();
        if (voci.isEmpty()) return "Nessuna elaborazione";
        Elaborazione u = voci.get(0);
        return "Ultima  ·  Rinominati " + u.rinominati() + "  ·  Non letti " + u.nonLetti() + "  ·  Errori " + u.errori();
    }

    // ---- Le viste del centro ----

    private void avvia(JobParams params) {
        Elaborazione e = session.avvia(params);
        setup.setOccupato(true);
        processPane = new ProcessPane(session, e);
        mostraElaborazione(e);
    }

    /** Sul thread FX. Chi stava guardando il lavoro si ritrova il riepilogo, gli altri restano li'. */
    private void elaborazioneFinita(Elaborazione fine) {
        if (processPane != null) processPane.fermaAnimazione();
        processPane = null;
        setup.setOccupato(false);
        if (!inImpostazioni && selezionata != null && selezionata == fine.iniziataIl()) {
            mostraElaborazione(fine);
        } else {
            renderStorico();
        }
    }

    private void mostraForm() {
        vai(setup.getRoot(), null, false);
    }

    private void mostraElaborazione(Elaborazione e) {
        boolean corrente = processPane != null && session.corrente() != null
                && session.corrente().iniziataIl() == e.iniziataIl();
        vai(corrente ? processPane.getRoot() : new RiepilogoPane(e, this::riusa).getRoot(), e.iniziataIl(), false);
    }

    private void mostraImpostazioni() {
        vai(new ImpostazioniPane(session, controlDir, this::renderStorico).getRoot(), null, true);
    }

    private void riusa(Elaborazione e) {
        setup.riempi(e);
        mostraForm();
    }

    private void vai(Parent vista, Long elaborazione, boolean impostazioni) {
        centro.setContent(vista);
        selezionata = elaborazione;
        inImpostazioni = impostazioni;
        // L'animazione gira solo a schermo: nascosta, costerebbe e basta.
        if (processPane != null) {
            if (vista == processPane.getRoot()) processPane.avviaAnimazione();
            else processPane.fermaAnimazione();
        }
        renderStorico();
    }

    // ---- Barra del titolo ----

    private Node costruisciBarraTitolo() {
        ImageView icona = new ImageView(Dialogs.icone()[2]);
        icona.setFitHeight(16);
        icona.setPreserveRatio(true);
        Label titolo = new Label(AppInfo.NAME + "  " + AppInfo.VERSION);
        titolo.getStyleClass().add("rp-title-text");

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);

        Button min = bottoneFinestra("win-minimize", () -> stage.setIconified(true), false);
        maxButton = bottoneFinestra("win-maximize", this::alternaIngrandita, false);
        Button chiudi = bottoneFinestra("close", stage::close, true);

        HBox sinistra = new HBox(10, icona, titolo);
        sinistra.setAlignment(Pos.CENTER_LEFT);
        sinistra.setPadding(new Insets(0, 0, 0, 12));

        HBox barra = new HBox(sinistra, spazio, new HBox(min, maxButton, chiudi));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("rp-titlebar");
        barra.setMinHeight(34);
        barra.setOnMousePressed(this::suPressioneTitolo);
        barra.setOnMouseDragged(this::suTrascinamentoTitolo);
        barra.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) alternaIngrandita();
        });
        return barra;
    }

    private Button bottoneFinestra(String icona, Runnable azione, boolean chiudi) {
        Button b = new Button();
        b.setGraphic(Icons.of(icona, 13));
        b.getStyleClass().add("rp-win-button");
        if (chiudi) b.getStyleClass().add("close");
        b.setFocusTraversable(false);
        b.setOnAction(e -> azione.run());
        return b;
    }

    private void suPressioneTitolo(MouseEvent e) {
        dragDX = e.getScreenX() - stage.getX();
        dragDY = e.getScreenY() - stage.getY();
    }

    private void suTrascinamentoTitolo(MouseEvent e) {
        if (maximized) {
            // da ingrandita: si ripristina e il puntatore resta alla stessa proporzione della barra
            double fx = (e.getScreenX() - stage.getX()) / stage.getWidth();
            alternaIngrandita();
            dragDX = stage.getWidth() * fx;
            dragDY = 12;
        }
        stage.setX(e.getScreenX() - dragDX);
        stage.setY(e.getScreenY() - dragDY);
    }

    /** Sui visual bounds: con UNDECORATED il maximized nativo coprirebbe la barra delle applicazioni. */
    private void alternaIngrandita() {
        if (!maximized) {
            savedBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            List<Screen> schermi = Screen.getScreensForRectangle(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Rectangle2D vb = (schermi.isEmpty() ? Screen.getPrimary() : schermi.get(0)).getVisualBounds();
            stage.setX(vb.getMinX());
            stage.setY(vb.getMinY());
            stage.setWidth(vb.getWidth());
            stage.setHeight(vb.getHeight());
            maximized = true;
        } else {
            if (savedBounds != null) {
                stage.setX(savedBounds.getMinX());
                stage.setY(savedBounds.getMinY());
                stage.setWidth(savedBounds.getWidth());
                stage.setHeight(savedBounds.getHeight());
            }
            maximized = false;
        }
        stage.getProperties().put("rp-maximized", maximized); // il WindowResizer si disattiva
        maxButton.setGraphic(Icons.of(maximized ? "win-restore" : "win-maximize", 13));
    }

    // ---- Barra di sinistra ----

    private Node costruisciBarraSinistra() {
        Button alterna = bottoneNav("menu", () -> {
            barraAperta = !barraAperta;
            aggiornaBarra();
        });
        nuovaButton = bottoneNav("plus", this::mostraForm);
        VBox cima = new VBox(4, alterna, nuovaButton);
        cima.setPadding(new Insets(8, 6, 4, 6));

        listaStorico.setPadding(new Insets(2, 6, 6, 6));
        ScrollPane scorri = new ScrollPane(listaStorico);
        scorri.setFitToWidth(true);
        scorri.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scorri.getStyleClass().add("rp-scroll");

        aggiornamentiButton = bottoneNav("refresh", controllaAggiornamenti);
        impostazioniButton = bottoneNav("settings", this::mostraImpostazioni);
        VBox fondo = new VBox(aggiornamentiButton, impostazioniButton);
        fondo.getStyleClass().add("rp-sidebar-sep");
        fondo.setPadding(new Insets(6, 6, 8, 6));

        sidebar.getStyleClass().add("rp-sidebar");
        sidebar.setTop(cima);
        sidebar.setCenter(scorri);
        sidebar.setBottom(fondo);
        aggiornaBarra();
        return sidebar;
    }

    private Button bottoneNav(String icona, Runnable azione) {
        Button b = new Button();
        b.setGraphic(Icons.of(icona, 18));
        b.getStyleClass().add("rp-nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setOnAction(e -> azione.run());
        return b;
    }

    private void aggiornaBarra() {
        double w = barraAperta ? LARGHEZZA_BARRA_APERTA : LARGHEZZA_BARRA_CHIUSA;
        sidebar.setPrefWidth(w);
        sidebar.setMinWidth(w);
        sidebar.setMaxWidth(w);
        etichetta(nuovaButton, "Nuova elaborazione");
        etichetta(aggiornamentiButton, "Controlla aggiornamenti");
        etichetta(impostazioniButton, "Impostazioni");
        renderStorico();
    }

    /** Chiusa, il testo diventa un tooltip: un'etichetta tagliata a meta' si legge come un difetto. */
    private void etichetta(Button b, String testo) {
        b.setText(barraAperta ? testo : "");
        b.setTooltip(barraAperta ? null : new Tooltip(testo));
    }

    private void renderStorico() {
        selezionato(nuovaButton, selezionata == null && !inImpostazioni);
        selezionato(impostazioniButton, inImpostazioni);
        List<Elaborazione> voci = session.storico();
        listaStorico.getChildren().clear();
        if (voci.isEmpty() && barraAperta) {
            Label vuoto = new Label("Le elaborazioni fatte compaiono qui.");
            vuoto.getStyleClass().add("rp-sidebar-empty");
            listaStorico.getChildren().add(vuoto);
        }
        for (Elaborazione e : voci) listaStorico.getChildren().add(voce(e));
    }

    private static void selezionato(Button b, boolean si) {
        if (si) {
            if (!b.getStyleClass().contains("selected")) b.getStyleClass().add("selected");
        } else {
            b.getStyleClass().remove("selected");
        }
    }

    private Node voce(Elaborazione e) {
        Region pallino = new Region();
        pallino.getStyleClass().setAll("rp-run-dot", classeStato(e));

        String quando = QUANDO.format(Instant.ofEpochMilli(e.iniziataIl()));
        String dettaglio = e.pacco() + "  •  " + e.esito() + "  •  " + quando + "\n" + e.input();

        Button b = new Button();
        b.getStyleClass().addAll("rp-nav-button", "rp-run-entry");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setTooltip(new Tooltip(dettaglio));
        b.setOnAction(ev -> mostraElaborazione(e));
        selezionato(b, !inImpostazioni && selezionata != null && selezionata == e.iniziataIl());
        if (barraAperta) {
            Label nome = new Label(e.pacco() + "  ·  " + e.esito());
            nome.getStyleClass().add("rp-run-alias");
            Label meta = new Label(quando + "  ·  " + esitoBreve(e));
            meta.getStyleClass().add("rp-run-meta");
            HBox riga = new HBox(8, pallino, new VBox(1, nome, meta));
            riga.setAlignment(Pos.CENTER_LEFT);
            b.setGraphic(riga);
        } else {
            b.setGraphic(pallino);
            b.setAlignment(Pos.CENTER);
        }
        return b;
    }

    private static String esitoBreve(Elaborazione e) {
        if (e.stato() == Elaborazione.Stato.IN_CORSO) return "in corso";
        return e.rinominati() + " ok, " + e.nonLetti() + " non letti, " + e.errori() + " err.";
    }

    private static String classeStato(Elaborazione e) {
        return switch (e.stato()) {
            case IN_CORSO -> "inprogress";
            case RIUSCITA -> "done";
            case NON_LETTI -> "warn";
            case VUOTA -> "empty";
            case ERRORE -> "error";
        };
    }

    // ---- Piede ----

    private Node costruisciPiede() {
        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        Label nome = new Label(AppInfo.NAME + "  •  " + AppInfo.VERSION);
        nome.getStyleClass().add("rp-footer-muted");
        HBox piede = new HBox(8, riepilogoPiede, spazio, nome);
        piede.setAlignment(Pos.CENTER_LEFT);
        piede.getStyleClass().add("rp-footer");
        return piede;
    }
}
