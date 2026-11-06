package app.core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

/**
 * Lettura barcode dalle immagini delle pagine PDF.
 *
 * Metodo primario: Scandit eseguito dentro WSL, identico a sorter-fx
 * (objReadBarcode): {@code wsl /srv/scandit_multiple/it.smaart.sorter.stock
 * test.key codes_ALL.settings /mnt/c/...png} con output JSON dal quale si
 * estrae {@code "value":"..."}.
 *
 * Fallback: ZXing in-process, usato quando WSL/Scandit non è disponibile.
 * Sui moduli integr@a (scansioni fax degradate) il detector a pagina intera
 * non trova il DataMatrix del codice invio: per questo il fallback scandisce
 * la pagina a finestre, partendo dalla zona in alto a destra dove il
 * DataMatrix si trova in questi moduli.
 */
public final class BarcodeReader {

    private BarcodeReader() {
    }

    private static final Pattern VALUE_PATTERN = Pattern.compile("\"value\":\"(.+?)\"");
    /**
     * Codici cercati come sottostringa del testo di un 2D, che puo' contenere dati aggiuntivi.
     * Solo le famiglie con lettere: un 12 o 17 cifre preso a meta' di una sequenza di cifre
     * piu' lunga sarebbe un codice inventato.
     */
    private static final List<Pattern> INNER_CODES = List.of(
            Pattern.compile("[A-Z]{2}\\d{10}"),
            Pattern.compile("A[BC]W\\d{8}(?!\\d)"));
    private static final int WSL_TIMEOUT_SECONDS = 60;

    private static volatile Boolean wslAvailable;

    /** Verifica (una sola volta) che WSL risponda e che Scandit sia installato. */
    public static boolean isWslAvailable() {
        Boolean cached = wslAvailable;
        if (cached != null) {
            return cached;
        }
        synchronized (BarcodeReader.class) {
            if (wslAvailable != null) {
                return wslAvailable;
            }
            boolean ok = false;
            try {
                ProcessBuilder pb = new ProcessBuilder("wsl", "ls",
                        Globals.SCANDIT_DIR + Globals.SCANDIT_PROGRAM);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                StringBuilder out = new StringBuilder();
                Thread reader = new Thread(() -> {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            out.append(line).append('\n');
                        }
                    } catch (Exception ignored) {
                    }
                });
                reader.setDaemon(true);
                reader.start();
                if (process.waitFor(20, TimeUnit.SECONDS)) {
                    ok = out.toString().contains(Globals.SCANDIT_DIR + Globals.SCANDIT_PROGRAM);
                } else {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                LogQueue.add("wsl", "verifica WSL fallita: " + e.getMessage());
            }
            wslAvailable = ok;
            LogQueue.add("wsl", ok ? "WSL + Scandit disponibili: uso Scandit"
                    : "WSL/Scandit non disponibili: uso il lettore di riserva ZXing");
            return ok;
        }
    }

    /** Legge i barcode di una pagina già salvata come PNG, via WSL/Scandit. */
    public static List<String> readWithWsl(Path png) {
        List<String> ret = new ArrayList<>();
        String wslPath = toWslPath(png);
        try {
            ProcessBuilder pb = new ProcessBuilder("wsl",
                    Globals.SCANDIT_DIR + Globals.SCANDIT_PROGRAM,
                    Globals.SCANDIT_DIR + Globals.SCANDIT_KEY,
                    Globals.SCANDIT_DIR + Globals.SCANDIT_SETTINGS,
                    wslPath);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = VALUE_PATTERN.matcher(line);
                    while (m.find()) {
                        addClean(ret, m.group(1));
                    }
                }
            }
            if (!process.waitFor(WSL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                LogQueue.add("wsl", "timeout Scandit su " + png.getFileName());
            }
        } catch (Exception e) {
            LogQueue.add("wsl", "errore Scandit su " + png.getFileName() + ": " + e.getMessage());
        }
        return ret;
    }

    /** Conversione percorso Windows -> WSL: C:\x\y -> /mnt/c/x/y (come sorter-fx). */
    static String toWslPath(Path path) {
        String p = path.toAbsolutePath().toString();
        char unity = p.charAt(0);
        return p.replace(unity + ":", "/mnt/" + Character.toLowerCase(unity)).replace("\\", "/");
    }

    /** Pulizia barcode identica a barcodeClean di sorter-fx. */
    static String clean(String barcode) {
        String ret = barcode.replace("-BACK", "").replace(" ", "").replace("\"", "");
        Matcher m = Pattern.compile("([0-9a-zA-Z]+)/.+").matcher(barcode);
        if (m.find()) {
            ret = m.group(1);
        }
        return ret.trim();
    }

    private static void addClean(List<String> list, String raw) {
        String barcode = clean(raw);
        if (!barcode.isBlank() && !list.contains(barcode)) {
            list.add(barcode);
        }
    }

    /**
     * Fallback ZXing su una pagina. Il DataMatrix del codice invio è
     * l'obiettivo: la scansione a finestre (zona alto-destra per prima) lo
     * trova in pochi secondi, quindi si tenta quella per prima e ci si ferma
     * appena esce un codice valido. Solo se fallisce si paga la lettura a
     * pagina intera dei lineari, a risoluzione dimezzata per contenerne il
     * costo (TRY_HARDER su tutta la pagina a 600 DPI costa decine di secondi).
     */
    public static List<String> readWithZxing(BufferedImage image) {
        List<String> ret = new ArrayList<>();
        String dm = scanDataMatrixWindows(image);
        if (dm != null) {
            ret.add(dm);
            return ret;
        }
        for (Result r : decodeMultiple(downscale(image, 2))) {
            addClean(ret, r.getText());
        }
        return ret;
    }

    private static BufferedImage downscale(BufferedImage src, int factor) {
        BufferedImage out = new BufferedImage(src.getWidth() / factor, src.getHeight() / factor,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    private static List<Result> decodeMultiple(BufferedImage image) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(
                BarcodeFormat.DATA_MATRIX, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39,
                BarcodeFormat.ITF, BarcodeFormat.QR_CODE));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        try {
            GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(new MultiFormatReader());
            return List.of(reader.decodeMultiple(bitmap, hints));
        } catch (NotFoundException notFound) {
            return List.of();
        }
    }

    /**
     * Scansione a finestre per DataMatrix: prima la zona in alto a destra
     * con griglia fine (posizione del codice invio nei moduli integr@a),
     * poi il resto della pagina. Si ferma alla prima decodifica valida.
     */
    private static String scanDataMatrixWindows(BufferedImage img) {
        String hit = scanGrid(img, 0.62, 0.90, 0.02, 0.32, 0.03, new double[]{0.18, 0.12});
        if (hit == null) {
            hit = scanGrid(img, 0.0, 0.95, 0.0, 0.95, 0.06, new double[]{0.18, 0.12});
        }
        return hit;
    }

    private static String scanGrid(BufferedImage img, double xFrom, double xTo,
                                   double yFrom, double yTo, double step, double[] sizes) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (double size : sizes) {
            for (double ys = yFrom; ys <= yTo; ys += step) {
                for (double xs = xFrom; xs <= xTo; xs += step) {
                    int x = Math.min((int) (w * xs), w - 1);
                    int y = Math.min((int) (h * ys), h - 1);
                    int side = (int) (w * size);
                    if (x + side > w) {
                        x = w - side;
                    }
                    if (y + side > h) {
                        y = h - side;
                    }
                    if (x < 0 || y < 0) {
                        continue;
                    }
                    BufferedImage sub = img.getSubimage(x, y, side, side);
                    if (darkRatio(sub) < 0.02) {
                        continue; // finestra quasi bianca: nessun codice qui
                    }
                    for (int scale : new int[]{1, 2}) {
                        String text = decodeDataMatrix(scale == 1 ? sub : upscale(sub, scale));
                        if (text != null) {
                            String cleaned = clean(text);
                            if (isRight(cleaned)) {
                                return cleaned;
                            }
                            String inner = innerCode(cleaned);
                            if (inner != null) {
                                return inner;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Stima veloce della densità di pixel scuri campionando 1 pixel su 8. */
    private static double darkRatio(BufferedImage img) {
        int dark = 0;
        int total = 0;
        for (int y = 0; y < img.getHeight(); y += 8) {
            for (int x = 0; x < img.getWidth(); x += 8) {
                if ((img.getRGB(x, y) & 0xFF) < 128) {
                    dark++;
                }
                total++;
            }
        }
        return total == 0 ? 0 : (double) dark / total;
    }

    private static String decodeDataMatrix(BufferedImage img) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.DATA_MATRIX));
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(img);
        try {
            return new MultiFormatReader().decode(
                    new BinaryBitmap(new HybridBinarizer(source)), hints).getText();
        } catch (Exception first) {
            try {
                return new MultiFormatReader().decode(
                        new BinaryBitmap(new GlobalHistogramBinarizer(source)), hints).getText();
            } catch (Exception second) {
                return null;
            }
        }
    }

    private static BufferedImage upscale(BufferedImage src, int factor) {
        BufferedImage out = new BufferedImage(src.getWidth() * factor, src.getHeight() * factor,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    /**
     * Seleziona il barcode giusto nell'ordine in cui sono stati trovati
     * (pagina 1 prima di pagina 2, DataMatrix prima dei lineari): prima un
     * match esatto di una delle {@link Globals#BARCODE_FAMILIES}, famiglia per
     * famiglia nell'ordine di preferenza, poi la ricerca come sottostringa
     * (i 2D possono contenere dati aggiuntivi).
     */
    public static String selectRight(List<String> barcodes) {
        for (Pattern family : Globals.BARCODE_FAMILIES) {
            for (String b : barcodes) {
                if (family.matcher(b).matches()) {
                    return b;
                }
            }
        }
        for (String b : barcodes) {
            String inner = innerCode(b);
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    static boolean isRight(String barcode) {
        for (Pattern family : Globals.BARCODE_FAMILIES) {
            if (family.matcher(barcode).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String innerCode(String barcode) {
        for (Pattern p : INNER_CODES) {
            Matcher m = p.matcher(barcode);
            if (m.find()) {
                return m.group();
            }
        }
        return null;
    }
}
