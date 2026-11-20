package app.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Log su file nella cartella di output, come nel risultato atteso:
 * LOG_OK.txt, LOG_ERRORI.txt, LOG_DATAMATRIX_NON_LETTI.txt.
 */
public final class FileLogs {

    private final Path okLog;
    private final Path errorLog;
    private final Path unreadLog;

    public FileLogs(Path outputFolder) throws IOException {
        okLog = outputFolder.resolve("LOG_OK.txt");
        errorLog = outputFolder.resolve("LOG_ERRORI.txt");
        unreadLog = outputFolder.resolve("LOG_DATAMATRIX_NON_LETTI.txt");
        Files.createDirectories(outputFolder);
        touch(okLog);
        touch(errorLog);
        touch(unreadLog);
    }

    private static void touch(Path p) throws IOException {
        if (!Files.exists(p)) {
            Files.createFile(p);
        }
    }

    public void ok(String sourceFile, String newName) {
        append(okLog, sourceFile + ";" + newName);
    }

    public void unread(String sourceFile, String reason) {
        append(unreadLog, sourceFile + ";" + reason);
    }

    public void error(String sourceFile, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        append(errorLog, sourceFile + ";" + e + System.lineSeparator() + sw);
    }

    private synchronized void append(Path file, String line) {
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.append(LocalDateTime.now().toString()).append(";").append(line)
                    .append(System.lineSeparator());
        } catch (IOException io) {
            LogQueue.add("FileLogs", "impossibile scrivere log: " + io.getMessage());
        }
    }
}
