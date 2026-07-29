package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class SystemMonitorController {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitorController.class);

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${teaching.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("uptime", getUptime());
        data.put("jvm", getJvmInfo());
        data.put("cpu", getCpuInfo());
        data.put("thread", getThreadInfo());
        data.put("dbPool", getDbPoolInfo());
        data.put("disk", getDiskInfo());
        data.put("gc", getGcInfo());
        return R.ok(data);
    }

    // ── 运行时长 ──
    private Map<String, Object> getUptime() {
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("startTime", mx.getStartTime());
        m.put("uptimeMs", mx.getUptime());
        m.put("uptimeDisplay", formatDuration(mx.getUptime()));
        return m;
    }

    // ── JVM 内存 ──
    private Map<String, Object> getJvmInfo() {
        MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mx.getHeapMemoryUsage();
        MemoryUsage nonHeap = mx.getNonHeapMemoryUsage();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jvmName", ManagementFactory.getRuntimeMXBean().getVmName());
        m.put("jvmVersion", ManagementFactory.getRuntimeMXBean().getVmVersion());

        Map<String, Object> heapMap = new LinkedHashMap<>();
        heapMap.put("initMB", toMB(heap.getInit()));
        heapMap.put("usedMB", toMB(heap.getUsed()));
        heapMap.put("committedMB", toMB(heap.getCommitted()));
        heapMap.put("maxMB", toMB(heap.getMax()));
        heapMap.put("usedPercent", heap.getMax() > 0
            ? Math.round((double) heap.getUsed() / heap.getMax() * 10000.0) / 100.0 : 0);
        m.put("heap", heapMap);

        Map<String, Object> nonHeapMap = new LinkedHashMap<>();
        nonHeapMap.put("usedMB", toMB(nonHeap.getUsed()));
        nonHeapMap.put("committedMB", toMB(nonHeap.getCommitted()));
        m.put("nonHeap", nonHeapMap);

        Runtime rt = Runtime.getRuntime();
        m.put("availableProcessors", rt.availableProcessors());

        return m;
    }

    // ── CPU ──
    private Map<String, Object> getCpuInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        m.put("name", os.getName());
        m.put("arch", os.getArch());
        m.put("availableProcessors", os.getAvailableProcessors());
        m.put("systemLoadAverage", os.getSystemLoadAverage());

        // JDK 9+ com.sun.management.OperatingSystemMXBean 扩展
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            m.put("processCpuLoad", Math.round(sunOs.getProcessCpuLoad() * 10000.0) / 100.0);
            m.put("systemCpuLoad", Math.round(sunOs.getSystemCpuLoad() * 10000.0) / 100.0);
            m.put("totalMemoryGB", round1(sunOs.getTotalMemorySize() / (1024.0 * 1024 * 1024)));
            m.put("freeMemoryGB", round1(sunOs.getFreeMemorySize() / (1024.0 * 1024 * 1024)));
        }
        return m;
    }

    // ── 线程 ──
    private Map<String, Object> getThreadInfo() {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("liveThreads", mx.getThreadCount());
        m.put("daemonThreads", mx.getDaemonThreadCount());
        m.put("peakThreads", mx.getPeakThreadCount());
        m.put("totalStartedThreads", mx.getTotalStartedThreadCount());
        return m;
    }

    // ── HikariCP 连接池 ──
    private Map<String, Object> getDbPoolInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "HikariCP");
        if (dataSource instanceof HikariDataSource hds) {
            m.put("poolName", hds.getPoolName());
            m.put("maximumPoolSize", hds.getMaximumPoolSize());
            m.put("minimumIdle", hds.getMinimumIdle());
            m.put("connectionTimeoutMs", hds.getConnectionTimeout());
            m.put("idleTimeoutMs", hds.getIdleTimeout());
            m.put("maxLifetimeMs", hds.getMaxLifetime());

            HikariPoolMXBean pool = hds.getHikariPoolMXBean();
            m.put("activeConnections", pool.getActiveConnections());
            m.put("idleConnections", pool.getIdleConnections());
            m.put("totalConnections", pool.getTotalConnections());
            m.put("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
        } else {
            m.put("note", "not HikariCP or not available");
        }
        return m;
    }

    // ── 磁盘 ──
    private Map<String, Object> getDiskInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir = new File(".");
            File partition = dir.getAbsoluteFile();
            while (partition.getParentFile() != null) {
                partition = partition.getParentFile();
            }
            // 用确切的路径
            File storeFile = dir.isDirectory() ? dir : dir.getParentFile();
            if (storeFile != null) {
                long total = storeFile.getTotalSpace();
                long free = storeFile.getUsableSpace();
                long used = total - free;
                m.put("path", storeFile.getAbsolutePath());
                m.put("totalGB", round1(total / (1024.0 * 1024 * 1024)));
                m.put("freeGB", round1(free / (1024.0 * 1024 * 1024)));
                m.put("usedGB", round1(used / (1024.0 * 1024 * 1024)));
                m.put("usedPercent", total > 0 ? Math.round((double) used / total * 10000.0) / 100.0 : 0);
                // 上传目录可写性
                m.put("writable", storeFile.canWrite());
            }
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    // ── GC ──
    private Map<String, Object> getGcInfo() {
        List<Map<String, Object>> list = new ArrayList<>();
        long totalGcCount = 0;
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", gc.getName());
            item.put("collectionCount", gc.getCollectionCount());
            item.put("collectionTimeMs", gc.getCollectionTime());
            list.add(item);
            if (gc.getCollectionCount() > 0) totalGcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) totalGcTime += gc.getCollectionTime();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gcList", list);
        m.put("totalGcCount", totalGcCount);
        m.put("totalGcTimeMs", totalGcTime);
        return m;
    }

    // ── 辅助 ──

    private static long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String formatDuration(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long day = hour / 24;
        return (day > 0 ? day + "天 " : "")
            + (hour % 24) + "时 " + (min % 60) + "分 " + (sec % 60) + "秒";
    }
}
