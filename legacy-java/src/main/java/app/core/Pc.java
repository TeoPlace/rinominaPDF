package app.core;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.management.OperatingSystemMXBean;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;

/**
 * Monitor risorse macchina (stesso metodo della classe Pc di sorter-fx):
 * CPU via OperatingSystemMXBean, disco via OSHI (tempo di trasferimento),
 * memoria via OSHI per il log-panel.
 */
public final class Pc {

    private Pc() {
    }

    public static final ConcurrentHashMap<String, Double> usage = new ConcurrentHashMap<>();

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static long lastTransferTime = -1;
    private static long lastSampleMillis = -1;

    public static void cpu() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double systemCpuLoad = osBean.getCpuLoad() * 100;
        if (systemCpuLoad >= 0) {
            usage.put("cpu", systemCpuLoad);
        }
    }

    /**
     * Percentuale di occupazione del disco di sistema, calcolata come tempo
     * di trasferimento accumulato tra due campionamenti.
     */
    public static synchronized void disk() {
        long transferTime = 0;
        for (HWDiskStore disk : SYSTEM_INFO.getHardware().getDiskStores()) {
            disk.updateAttributes();
            transferTime += disk.getTransferTime();
        }
        long now = System.currentTimeMillis();
        if (lastTransferTime >= 0 && now > lastSampleMillis) {
            double busyPercentage = 100.0 * (transferTime - lastTransferTime) / (now - lastSampleMillis);
            usage.put("disk", Math.max(0, Math.min(100, busyPercentage)));
        }
        lastTransferTime = transferTime;
        lastSampleMillis = now;
    }

    public static void memory() {
        GlobalMemory mem = SYSTEM_INFO.getHardware().getMemory();
        double usedPercent = 100.0 * (mem.getTotal() - mem.getAvailable()) / mem.getTotal();
        usage.put("memory", usedPercent);
        usage.put("memoryUsedMb", (mem.getTotal() - mem.getAvailable()) / 1024.0 / 1024.0);
        usage.put("memoryTotalMb", mem.getTotal() / 1024.0 / 1024.0);
        Runtime rt = Runtime.getRuntime();
        usage.put("jvmUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0);
    }

    public static double get(String key) {
        return usage.getOrDefault(key, 0.0);
    }
}
