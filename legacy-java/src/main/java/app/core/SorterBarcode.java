package app.core;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.net.URISyntaxException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Il lettore di barcode: {@code sorter_barcode.dll}, la libreria C di sorter-barcode
 * (<a href="https://github.com/TeoPlace/lib_sorter_barcode_windows">lib_sorter_barcode_windows</a>),
 * la stessa di SorterNext e SorterBarcode, chiamata dentro il processo con la Foreign Function API.
 *
 * <p>La dll viaggia nel pacchetto accanto al fat-jar ({@code <installazione>\app\}); in sviluppo e nei
 * test si prende da {@code lib/sorter-barcode/} del repo. Si cerca e si carica una volta sola; se non
 * c'e', {@link #available()} e' falso e {@link #loadError()} dice perche'.
 *
 * <p>Le funzioni sono thread-safe (lo stato di ogni lettura e' per-thread nella libreria): i worker di
 * {@link RenameJob} la chiamano insieme.
 */
public final class SorterBarcode {

    private SorterBarcode() {
    }

    /** Nome della dll, nel pacchetto e in {@code lib/sorter-barcode/}. */
    public static final String DLL = "sorter_barcode.dll";

    /** Tutte le simbologie. */
    private static final String SETTINGS = "ALL";
    private static final int FIRST_CAPACITY = 16 * 1024;
    private static final int ERR_TOO_SMALL = -3;

    private static final Pattern VALUE = Pattern.compile("\"value\":\"((?:\\\\.|[^\"\\\\])*)\"");

    private static volatile Lib lib;
    private static volatile String loadError;

    private record Lib(Path path, MethodHandle version, MethodHandle readFile, MethodHandle readGray) {
    }

    /** True se la dll e' stata trovata e caricata. */
    public static boolean available() {
        return lib() != null;
    }

    /** Perche' la dll non e' disponibile, oppure null. */
    public static String loadError() {
        lib();
        return loadError;
    }

    /** Dove e' stata caricata la dll, oppure null. */
    public static Path dllPath() {
        Lib l = lib();
        return l == null ? null : l.path();
    }

    /** {@code sb_version()}, per esempio "1.2.0"; null se la dll non c'e'. */
    public static String version() {
        Lib l = lib();
        if (l == null) {
            return null;
        }
        try {
            MemorySegment s = (MemorySegment) l.version().invokeExact();
            return s.reinterpret(64).getString(0);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Legge un PDF (le immagini di tutte le pagine) o un BMP con {@code sb_read_file}. Un PDF senza
     * immagini che la libreria sa aprire (codici disegnati come vettori o testo, JBIG2, JPEG 2000,
     * PDF cifrati) per la libreria e' un file illeggibile: errore -2.
     *
     * @return i valori dei codici, nell'ordine dato dalla libreria, ognuno una volta
     * @throws ReadException se la dll non c'e', il percorso non si puo' passare alla libreria
     *                       ({@link #canPass(Path)}) o la lettura restituisce un errore
     */
    public static List<String> readFile(Path file, boolean fast) throws ReadException {
        Lib l = require();
        byte[] path = encodePath(file);
        if (path == null) {
            throw new ReadException("percorso con caratteri che la libreria non puo' aprire: " + file);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cPath = arena.allocate(path.length + 1L);
            MemorySegment.copy(path, 0, cPath, ValueLayout.JAVA_BYTE, 0, path.length);
            MemorySegment settings = arena.allocateFrom(SETTINGS);
            return call(arena, (out, cap, needed) -> (int) l.readFile().invokeExact(
                    cPath, settings, fast ? 1 : 0, out, cap, needed));
        }
    }

    /**
     * Legge pixel grigi a 8 bit gia' in memoria con {@code sb_read_gray}: righe dall'alto,
     * {@code width} byte per riga.
     */
    public static List<String> readGray(byte[] pixels, int width, int height, boolean fast) throws ReadException {
        Lib l = require();
        if (width <= 0 || height <= 0 || pixels.length < (long) width * height) {
            throw new ReadException("immagine non valida " + width + "x" + height);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment px = arena.allocate((long) width * height);
            MemorySegment.copy(pixels, 0, px, ValueLayout.JAVA_BYTE, 0, width * height);
            MemorySegment settings = arena.allocateFrom(SETTINGS);
            return call(arena, (out, cap, needed) -> (int) l.readGray().invokeExact(
                    px, width, height, width, settings, fast ? 1 : 0, out, cap, needed));
        }
    }

    /**
     * True se il percorso si puo' dare alla libreria: la dll apre i file con {@code fopen}, cioe' con la
     * codepage ANSI di Windows, quindi un nome con caratteri fuori da quella codepage non si apre.
     */
    public static boolean canPass(Path file) {
        return encodePath(file) != null;
    }

    static byte[] encodePath(Path file) {
        String p = file.toAbsolutePath().toString();
        try {
            var enc = ansi().newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            var buf = enc.encode(java.nio.CharBuffer.wrap(p));
            byte[] out = new byte[buf.remaining()];
            buf.get(out);
            for (byte b : out) {
                if (b == 0) {
                    return null;
                }
            }
            return out;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static Charset ansi() {
        try {
            return Charset.forName(System.getProperty("native.encoding", "windows-1252"));
        } catch (Exception e) {
            return StandardCharsets.US_ASCII;
        }
    }

    /** Estrae i valori dal testo della libreria (una riga JSON per codice). */
    static List<String> parseValues(String text) {
        List<String> ret = new ArrayList<>();
        Matcher m = VALUE.matcher(text);
        while (m.find()) {
            String v = unescape(m.group(1));
            if (!ret.contains(v)) {
                ret.add(v);
            }
        }
        return ret;
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                b.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 'r' -> b.append('\r');
                case 't' -> b.append('\t');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> {
                    if (i + 4 < s.length()) {
                        b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> b.append(n);
            }
        }
        return b.toString();
    }

    @FunctionalInterface
    private interface Reader {
        int read(MemorySegment out, int cap, MemorySegment needed) throws Throwable;
    }

    /** Chiama la funzione di lettura, ripetendola una volta con il buffer della misura chiesta. */
    private static List<String> call(Arena arena, Reader reader) throws ReadException {
        MemorySegment needed = arena.allocate(ValueLayout.JAVA_INT);
        int cap = FIRST_CAPACITY;
        for (int attempt = 0; attempt < 2; attempt++) {
            MemorySegment out = arena.allocate(cap);
            int rc;
            try {
                rc = reader.read(out, cap, needed);
            } catch (Throwable t) {
                throw new ReadException("chiamata alla libreria fallita: " + t);
            }
            if (rc >= 0) {
                return parseValues(out.getString(0, StandardCharsets.UTF_8));
            }
            if (rc != ERR_TOO_SMALL) {
                throw new ReadException(describe(rc));
            }
            cap = Math.max(cap * 2, needed.get(ValueLayout.JAVA_INT, 0));
        }
        throw new ReadException(describe(ERR_TOO_SMALL));
    }

    static String describe(int rc) {
        return switch (rc) {
            case -1 -> "argomenti non validi (-1)";
            case -2 -> "file illeggibile (-2)";
            case -3 -> "risposta troppo lunga (-3)";
            case -4 -> "memoria esaurita (-4)";
            default -> "errore " + rc;
        };
    }

    private static Lib require() throws ReadException {
        Lib l = lib();
        if (l == null) {
            throw new ReadException("lettore non disponibile: " + loadError);
        }
        return l;
    }

    private static Lib lib() {
        Lib l = lib;
        if (l != null || loadError != null) {
            return l;
        }
        synchronized (SorterBarcode.class) {
            if (lib != null || loadError != null) {
                return lib;
            }
            Path dll = locate();
            if (dll == null) {
                loadError = DLL + " non trovata (cercata in " + candidates() + ")";
                LogQueue.add("lettore", loadError);
                return null;
            }
            try {
                Linker linker = Linker.nativeLinker();
                SymbolLookup symbols = SymbolLookup.libraryLookup(dll, Arena.global());
                MethodHandle version = linker.downcallHandle(symbols.find("sb_version").orElseThrow(),
                        FunctionDescriptor.of(ValueLayout.ADDRESS));
                MethodHandle readFile = linker.downcallHandle(symbols.find("sb_read_file").orElseThrow(),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                MethodHandle readGray = linker.downcallHandle(symbols.find("sb_read_gray").orElseThrow(),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                lib = new Lib(dll, version, readFile, readGray);
                LogQueue.add("lettore", "sorter_barcode " + version() + " da " + dll);
                return lib;
            } catch (Throwable t) {
                loadError = "impossibile caricare " + dll + ": " + t;
                LogQueue.add("lettore", loadError);
                return null;
            }
        }
    }

    private static Path locate() {
        for (Path p : candidates()) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    /**
     * Dove si cerca la dll, in ordine: la proprieta' {@code rinominapdf.sorterbarcode} (un file), accanto
     * al jar in esecuzione (il pacchetto), {@code lib/sorter-barcode/} della cartella di lavoro (sviluppo).
     */
    static List<Path> candidates() {
        List<Path> ret = new ArrayList<>();
        String forced = System.getProperty("rinominapdf.sorterbarcode");
        if (forced != null && !forced.isBlank()) {
            ret.add(Path.of(forced));
        }
        try {
            var src = SorterBarcode.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                Path here = Path.of(src.getLocation().toURI());
                Path dir = Files.isRegularFile(here) ? here.getParent() : here;
                if (dir != null) {
                    ret.add(dir.resolve(DLL));
                }
            }
        } catch (URISyntaxException | RuntimeException ignored) {
        }
        ret.add(Path.of("lib", "sorter-barcode", DLL));
        return ret;
    }

    /** Lettura non riuscita: il messaggio va nel log del file. */
    public static final class ReadException extends Exception {
        public ReadException(String message) {
            super(message);
        }
    }
}
