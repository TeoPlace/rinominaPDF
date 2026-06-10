package app.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Orchestratore della rinomina: coda di PDF, pool di thread a dimensione
 * dinamica regolata su CPU/disco (stesso meccanismo refreshThreads di
 * sorter-fx: +1 thread se CPU&lt;90% e disco&lt;90%, -1 altrimenti, minimo 1).
 */
public final class RenameJob {

    /** Callback verso la UI (chiamate da thread di lavoro, non dal FX thread). */
    public interface Listener {
        void onTotal(int totalFiles);

        void onProgress(int done, int total);

        void onFinished(int ok, int unread, int errors, boolean aborted);
    }

    /** Stato corrente di ogni thread di lavoro, mostrato nel log-panel. */
    public static final ConcurrentHashMap<String, String> threadStatus = new ConcurrentHashMap<>();

    private final JobParams params;
    private final Listener listener;
    private final ConcurrentLinkedQueue<Path> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger okCount = new AtomicInteger();
    private final AtomicInteger unreadCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final AtomicInteger workerSeq = new AtomicInteger();
    private volatile boolean aborted;

    private FileLogs logs;
    private Path tempDir;
    private boolean useWsl;

    public RenameJob(JobParams params, Listener listener) {
        this.params = params;
        this.listener = listener;
    }

    public void abort() {
        aborted = true;
    }

    /** Avvia il lavoro su un thread dedicato (non blocca il chiamante). */
    public Thread start() {
        Thread t = new Thread(this::run, "rename-job");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private void run() {
        threadStatus.clear();
        Globals.totalThreads = 1;
        Globals.processed.set(0);
        try {
            logs = new FileLogs(params.output());
            tempDir = params.output().resolve(".rinominapdf_tmp");
            Files.createDirectories(tempDir);

            try (Stream<Path> s = Files.list(params.input())) {
                s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(queue::add);
            }
            int total = queue.size();
            listener.onTotal(total);
            LogQueue.add("job", "trovati " + total + " PDF in " + params.input());
            if (total == 0) {
                listener.onFinished(0, 0, 0, false);
                return;
            }

            useWsl = BarcodeReader.isWslAvailable();

            List<Thread> threads = new ArrayList<>();
            while ((!queue.isEmpty() || !threads.isEmpty()) && !aborted) {
                while (!aborted && threads.size() < Globals.totalThreads && !queue.isEmpty()) {
                    Path pdf = queue.poll();
                    if (pdf == null) {
                        break;
                    }
                    Thread worker = newWorker(pdf, total);
                    threads.add(worker);
                    worker.start();
                }
                refreshThreads(threads);
                Thread.sleep(100);
            }
            for (Thread t : threads) {
                t.join();
            }
        } catch (Exception e) {
            LogQueue.add("job", "errore fatale: " + e.getMessage());
            if (logs != null) {
                logs.error("JOB", e);
            }
        } finally {
            cleanTempDir();
            threadStatus.clear();
            listener.onFinished(okCount.get(), unreadCount.get(), errorCount.get(), aborted);
            LogQueue.add("job", "fine: ok=" + okCount + " nonLetti=" + unreadCount
                    + " errori=" + errorCount + (aborted ? " (interrotto)" : ""));
        }
    }

    private Thread newWorker(Path pdf, int total) {
        Thread t = new Thread(() -> {
            String name = Thread.currentThread().getName();
            threadStatus.put(name, pdf.getFileName().toString());
            try {
                processOne(pdf);
            } finally {
                threadStatus.remove(name);
                listener.onProgress(Globals.processed.incrementAndGet(), total);
            }
        }, "worker-" + workerSeq.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    /**
     * Regolazione dinamica del numero di thread, come sorter-fx:
     * rimuove i thread terminati e adatta totalThreads alle risorse.
     */
    private void refreshThreads(List<Thread> threads) {
        Iterator<Thread> it = threads.iterator();
        while (it.hasNext()) {
            if (!it.next().isAlive()) {
                it.remove();
            }
        }
        Pc.cpu();
        if (Pc.get("cpu") < 90.00 && Pc.get("disk") < 90.00) {
            Globals.totalThreads += 1;
        } else if (Globals.totalThreads > 1) {
            Globals.totalThreads -= 1;
        }
    }

    private void processOne(Path pdf) {
        String fileName = pdf.getFileName().toString();
        try {
            List<String> barcodes = new ArrayList<>();
            if (useWsl) {
                for (Path png : PdfRenderer.renderToPng(pdf, tempDir)) {
                    for (String b : BarcodeReader.readWithWsl(png)) {
                        if (!barcodes.contains(b)) {
                            barcodes.add(b);
                        }
                    }
                    Files.deleteIfExists(png);
                }
            } else {
                for (BufferedImage page : PdfRenderer.renderPages(pdf, Globals.RENDER_DPI_ZXING)) {
                    for (String b : BarcodeReader.readWithZxing(page)) {
                        if (!barcodes.contains(b)) {
                            barcodes.add(b);
                        }
                    }
                    // il codice invio è in pagina 1: se è già uscito, inutile
                    // pagare la scansione a finestre delle pagine successive
                    if (BarcodeReader.selectRight(barcodes) != null) {
                        break;
                    }
                }
            }

            String barcode = BarcodeReader.selectRight(barcodes);
            if (barcode == null) {
                unreadCount.incrementAndGet();
                logs.unread(fileName, "barcode non leggibile, letti: " + barcodes);
                LogQueue.add("barcode", fileName + " -> NON LETTO (trovati: " + barcodes + ")");
                return;
            }

            Path target = uniqueTarget(params.buildFileName(barcode));
            Files.copy(pdf, target, StandardCopyOption.COPY_ATTRIBUTES);
            okCount.incrementAndGet();
            logs.ok(fileName, target.getFileName().toString());
            LogQueue.add("rename", fileName + " -> " + target.getFileName());
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logs.error(fileName, e);
            LogQueue.add("errore", fileName + ": " + e.getMessage());
        }
    }

    /** Evita collisioni se due PDF producono lo stesso nome. */
    private synchronized Path uniqueTarget(String name) {
        Path target = params.output().resolve(name);
        int n = 1;
        while (Files.exists(target)) {
            target = params.output().resolve(name.replaceAll("(?i)\\.pdf$", "") + "_" + n + ".pdf");
            n++;
        }
        return target;
    }

    private void cleanTempDir() {
        if (tempDir == null) {
            return;
        }
        try (Stream<Path> s = Files.list(tempDir)) {
            s.forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
        tempDir.toFile().delete();
    }
}
