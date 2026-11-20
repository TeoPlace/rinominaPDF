package app.tools;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import app.core.JobParams;
import app.core.LogQueue;
import app.core.RenameJob;

/**
 * Esegue il flusso completo di rinomina senza UI (per test/automazione):
 * java ... app.tools.EseguiJob <input> <output> <esito> <istat> <anno> <pacco>
 */
public final class EseguiJob {

    private EseguiJob() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Uso: EseguiJob <input> <output> <esito> <istat> <anno> <pacco>");
            System.exit(1);
        }
        JobParams params = new JobParams(Path.of(args[0]), Path.of(args[1]),
                args[2], JobParams.padIstat(args[3]), args[4], args[5]);
        CountDownLatch done = new CountDownLatch(1);
        RenameJob job = new RenameJob(params, new RenameJob.Listener() {
            @Override
            public void onTotal(int totalFiles) {
                System.out.println("file da elaborare: " + totalFiles);
            }

            @Override
            public void onProgress(int doneCount, int total) {
                if (doneCount % 10 == 0 || doneCount == total) {
                    System.out.println(doneCount + "/" + total);
                }
            }

            @Override
            public void onFinished(int ok, int unread, int errors, boolean aborted) {
                System.out.println("FINE ok=" + ok + " nonLetti=" + unread
                        + " errori=" + errors + " interrotto=" + aborted);
                done.countDown();
            }
        });
        Thread flusher = new Thread(() -> {
            while (done.getCount() > 0) {
                LogQueue.drain().forEach(System.out::println);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        flusher.setDaemon(true);
        flusher.start();
        job.start();
        done.await();
        LogQueue.drain().forEach(System.out::println);
    }
}
