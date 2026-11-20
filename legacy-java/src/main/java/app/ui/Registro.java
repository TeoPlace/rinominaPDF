package app.ui;

import app.core.Pc;
import app.core.RenameJob;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Il registro a destra: i numeri in cima, le righe sotto, collassabile (vedi STANDARD.md). Adattato
 * dal ThreadLog di listBigStorage. Un elenco piu' corto del previsto non si distingue da un lavoro
 * andato bene: i non letti e gli errori sono l'unica cosa che spiega la differenza.
 *
 * <p>Il filtro nasconde invece di selezionare, e le righe NON inseguono l'ultima: vedi
 * {@link #installaBloccoScorrimento()}.
 */
public final class Registro {

    private static final int LIMITE_TESTO = 400_000;

    private final AppSession session;
    private final VBox panel = new VBox();
    private final HBox header = new HBox(6);
    private final VBox body = new VBox(10);
    private final VBox barraChiusa = new VBox(6);
    private final Region spazio = new Region();
    private final Label titolo = new Label("Registro");
    private final Button collassa = new Button();
    private final MenuButton filtro;

    private final Label numRinominati = numero("ok");
    private final Label numNonLetti = numero("warn");
    private final Label numErrori = numero("crit");
    private final Gauge cpu = new Gauge();
    private final Gauge ram = new Gauge();
    private final Gauge disco = new Gauge();
    private final Label lavoratori = new Label("0");
    private final TextArea righe = new TextArea();

    private final Set<LogCategory> nascoste = EnumSet.noneOf(LogCategory.class);
    private boolean chiuso;
    private double pinnedTop;
    private boolean suppressPin;
    private boolean userGesture;

    public Registro(AppSession session) {
        this.session = session;

        collassa.setGraphic(Icons.of("collapse-right", 14));
        collassa.getStyleClass().add("rp-icon-button");
        collassa.setFocusTraversable(false);
        collassa.setTooltip(new Tooltip("Apri o chiudi il registro"));
        collassa.setOnAction(e -> alterna());

        filtro = creaFiltro();
        titolo.getStyleClass().add("rp-section-title");
        HBox.setHgrow(spazio, Priority.ALWAYS);
        header.getChildren().setAll(collassa, titolo, spazio, filtro);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 8, 4, 8));

        Label ultima = new Label("Ultima elaborazione");
        ultima.getStyleClass().add("rp-gauge-label");
        HBox numeri = new HBox(8,
                casella(numRinominati, "Rinominati"),
                casella(numNonLetti, "Non letti"),
                casella(numErrori, "Errori"));

        VBox misure = new VBox(5,
                riga("CPU", cpu.node()),
                riga("RAM", ram.node()),
                riga("Disco", disco.node()),
                riga("Lavoratori", lavoratori));

        righe.setEditable(false);
        righe.setWrapText(true);
        righe.getStyleClass().add("rp-log-area");
        VBox.setVgrow(righe, Priority.ALWAYS);
        installaBloccoScorrimento();

        body.getChildren().addAll(ultima, numeri, misure, righe);
        body.setPadding(new Insets(4, 10, 10, 10));
        VBox.setVgrow(body, Priority.ALWAYS);

        barraChiusa.setAlignment(Pos.TOP_CENTER);
        barraChiusa.setPadding(new Insets(8, 0, 0, 0));
        barraChiusa.getStyleClass().add("rp-collapsed-bar");
        barraChiusa.setVisible(false);
        barraChiusa.setManaged(false);

        panel.getChildren().addAll(header, body, barraChiusa);
        panel.getStyleClass().add("rp-registro");
        panel.setPrefWidth(320);
        panel.setMinWidth(Region.USE_PREF_SIZE);
        panel.setMaxWidth(Region.USE_PREF_SIZE);
    }

    public Node node() {
        return panel;
    }

    private static Label numero(String classe) {
        Label l = new Label("0");
        l.getStyleClass().addAll("rp-kpi-value", classe);
        return l;
    }

    private static VBox casella(Label valore, String etichetta) {
        Label l = new Label(etichetta);
        l.getStyleClass().add("rp-gauge-label");
        VBox box = new VBox(1, valore, l);
        box.getStyleClass().add("rp-kpi-card");
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static HBox riga(String nome, Node valore) {
        Label l = new Label(nome);
        l.getStyleClass().add("rp-gauge-label");
        l.setMinWidth(62);
        HBox row = new HBox(8, l, valore);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valore, Priority.ALWAYS);
        return row;
    }

    /** Chiuso resta una striscia con le due icone: i nodi hanno un solo padre, setAll li sposta. */
    private void alterna() {
        chiuso = !chiuso;
        body.setVisible(!chiuso);
        body.setManaged(!chiuso);
        header.setVisible(!chiuso);
        header.setManaged(!chiuso);
        barraChiusa.setVisible(chiuso);
        barraChiusa.setManaged(chiuso);
        panel.setPrefWidth(chiuso ? 46 : 320);
        collassa.setGraphic(Icons.of(chiuso ? "collapse-left" : "collapse-right", 14));
        if (chiuso) {
            barraChiusa.getChildren().setAll(collassa, filtro);
        } else {
            barraChiusa.getChildren().clear();
            header.getChildren().setAll(collassa, titolo, spazio, filtro);
        }
    }

    private MenuButton creaFiltro() {
        MenuButton mb = new MenuButton();
        mb.setGraphic(Icons.of("filter", 14));
        mb.getStyleClass().add("rp-icon-button");
        mb.setFocusTraversable(false);
        mb.setTooltip(new Tooltip("Nascondi le righe di una categoria"));
        for (LogCategory c : LogCategory.values()) {
            CheckBox cb = new CheckBox(c.etichetta);
            cb.setSelected(true);
            cb.selectedProperty().addListener((o, was, now) -> {
                if (now) nascoste.remove(c); else nascoste.add(c);
                ridisegna();
            });
            CustomMenuItem item = new CustomMenuItem(cb);
            item.setHideOnClick(false); // si possono togliere piu' categorie di fila
            mb.getItems().add(item);
        }
        return mb;
    }

    /** Dal flusher, sul thread FX: le righe arrivate si aggiungono in blocco, non una alla volta. */
    public void aggiorna() {
        List<LogBuffer.Entry> nuove = session.svuotaLog();
        if (!nuove.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (LogBuffer.Entry e : nuove) {
                if (!nascoste.contains(e.category())) sb.append(e.line());
            }
            if (!sb.isEmpty()) righe.appendText(sb.toString());
            if (righe.getLength() > LIMITE_TESTO) ridisegna();
        }
        numRinominati.setText(String.valueOf(session.rinominati()));
        numNonLetti.setText(String.valueOf(session.nonLetti()));
        numErrori.setText(String.valueOf(session.errori()));
        // Senza misura la barra resta su "—": uno 0% si leggerebbe come un dato.
        if (Pc.usage.containsKey("cpu")) cpu.percentuale(Pc.get("cpu"));
        if (Pc.usage.containsKey("memory")) ram.percentuale(Pc.get("memory"));
        if (Pc.usage.containsKey("disk")) disco.percentuale(Pc.get("disk"));
        lavoratori.setText(String.valueOf(RenameJob.threadStatus.size()));
    }

    /** Filtro cambiato o testo troppo lungo: si riparte dal buffer e si va in fondo una volta. */
    private void ridisegna() {
        StringBuilder sb = new StringBuilder();
        for (LogBuffer.Entry e : session.log().snapshot()) {
            if (!nascoste.contains(e.category())) sb.append(e.line());
        }
        righe.setText(sb.toString());
        Platform.runLater(() -> {
            suppressPin = true;
            righe.setScrollTop(Double.MAX_VALUE);
            pinnedTop = righe.getScrollTop();
            suppressPin = false;
        });
    }

    /**
     * Una TextArea scorre al CARET, che appendText porta in fondo, e lo fa anche in differita durante
     * il layout: ripristinare la posizione dopo l'append perde la corsa. Si ascolta quindi lo
     * scrollTop e si annulla qualunque scorrimento non voluto; i gesti dell'utente (rotella, barra,
     * tasti) aggiornano invece la posizione da tenere. Copiato da ThreadLog di listBigStorage.
     */
    private void installaBloccoScorrimento() {
        righe.scrollTopProperty().addListener((o, ov, nv) -> {
            if (suppressPin) return;
            if (userGesture) {
                pinnedTop = nv.doubleValue();
                return;
            }
            if (Math.abs(nv.doubleValue() - pinnedTop) > 0.5) {
                suppressPin = true;
                righe.setScrollTop(pinnedTop);
                suppressPin = false;
            }
        });
        righe.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> userGesture = true);
        righe.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> fineGesto());
        righe.addEventFilter(ScrollEvent.SCROLL, e -> {
            userGesture = true;
            fineGesto();
        });
        righe.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            userGesture = true;
            fineGesto();
        });
    }

    private void fineGesto() {
        Platform.runLater(() -> {
            pinnedTop = righe.getScrollTop();
            userGesture = false;
        });
    }

    private static final class Gauge {
        private final StackPane track = new StackPane();
        private final Region fill = new Region();
        private final Label value = new Label("—");
        private final SimpleDoubleProperty ratio = new SimpleDoubleProperty(0);
        private final HBox node;

        Gauge() {
            track.getStyleClass().add("rp-gauge-track");
            track.setMinHeight(12);
            track.setPrefHeight(12);
            fill.getStyleClass().add("rp-gauge-fill");
            fill.prefWidthProperty().bind(track.widthProperty().multiply(ratio));
            fill.setMaxWidth(Region.USE_PREF_SIZE);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);
            HBox.setHgrow(track, Priority.ALWAYS);
            value.setMinWidth(36);
            value.setAlignment(Pos.CENTER_RIGHT);
            value.getStyleClass().add("rp-gauge-value");
            node = new HBox(6, track, value);
            node.setAlignment(Pos.CENTER_LEFT);
        }

        HBox node() {
            return node;
        }

        void percentuale(double p) {
            ratio.set(Math.max(0, Math.min(100, p)) / 100);
            value.setText(Math.round(p) + "%");
            fill.getStyleClass().removeAll("warn", "crit");
            if (p >= 90) fill.getStyleClass().add("crit");
            else if (p >= 75) fill.getStyleClass().add("warn");
        }
    }
}
