package app.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo storico delle elaborazioni, in {@code elaborazioni.json} nella cartella di controllo, dalla
 * piu' recente. Un file rotto vale storico vuoto: non e' un motivo per non aprire il programma.
 */
public final class StoricoStore {

    private static final System.Logger log = System.getLogger(StoricoStore.class.getName());
    static final int MASSIMO = 200;

    private final ObjectMapper json = new ObjectMapper();
    private final Path file;

    public StoricoStore(Path controlDir) {
        this.file = controlDir.resolve("elaborazioni.json");
    }

    /** Una voce rimasta "in corso" viene da un programma morto a meta': si legge come interrotta. */
    public List<Elaborazione> carica() {
        List<Elaborazione> out = new ArrayList<>();
        if (!Files.isRegularFile(file)) return out;
        try {
            for (Elaborazione e : json.readValue(file.toFile(), new TypeReference<List<Elaborazione>>() {})) {
                out.add(e.finitaIl() != 0 ? e
                        : e.fine(e.rinominati(), e.nonLetti(), e.errori(), true, e.iniziataIl()));
            }
        } catch (IOException e) {
            log.log(System.Logger.Level.WARNING, "Storico illeggibile: " + e);
        }
        return out;
    }

    /** Scrive su un temporaneo e poi rinomina: un'interruzione non lascia un file a meta'. */
    public void salva(List<Elaborazione> voci) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            json.writerWithDefaultPrettyPrinter()
                    .writeValue(tmp.toFile(), voci.subList(0, Math.min(MASSIMO, voci.size())));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.log(System.Logger.Level.WARNING, "Storico non salvato: " + e);
        }
    }
}
