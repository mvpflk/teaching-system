package com.school.teaching.scheduler;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统监控定时采集 — 每 5 分钟将关键指标写入 logs/monitor.log。
 *
 * 使用独立 Logger "MonitorLogger"，由 logback-spring.xml 的 MONITOR_LOG appender 输出到单独文件。
 */
@Component
@ConditionalOnProperty(name = "monitor.log-enabled", havingValue = "true", matchIfMissing = true)
public class MonitorScheduler {

    private static final Logger monitorLog = LoggerFactory.getLogger("MonitorLogger");

    @Autowired(required = false)
    private DataSource dataSource;

    @Scheduled(fixedRate = 300_000)
    public void collectMetrics() {
        try {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("timestamp", Instant.now().toString());

            // JVM 内存
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            var heap = mem.getHeapMemoryUsage();
            metrics.put("heapUsedMB", heap.getUsed() / (1024 * 1024));
            metrics.put("heapMaxMB", heap.getMax() / (1024 * 1024));
            metrics.put("heapPercent", heap.getMax() > 0
                ? Math.round((double) heap.getUsed() / heap.getMax() * 10000.0) / 100.0 : 0);

            // CPU
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                metrics.put("processCpuLoad",
                    Math.round(sunOs.getProcessCpuLoad() * 10000.0) / 100.0);
            }
            metrics.put("systemLoadAverage", os.getSystemLoadAverage());

            // GC
            long totalGcCount = 0;
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                if (gc.getCollectionCount() > 0) totalGcCount += gc.getCollectionCount();
                if (gc.getCollectionTime() > 0) totalGcTime += gc.getCollectionTime();
            }
            metrics.put("gcCount", totalGcCount);
            metrics.put("gcTimeMs", totalGcTime);

            // 连接池
            if (dataSource instanceof HikariDataSource hds) {
                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                metrics.put("dbActive", pool.getActiveConnections());
                metrics.put("dbIdle", pool.getIdleConnections());
                metrics.put("dbTotal", pool.getTotalConnections());
                metrics.put("dbWaiting", pool.getThreadsAwaitingConnection());
            }

            // 线程
            metrics.put("threads", ManagementFactory.getThreadMXBean().getThreadCount());

            monitorLog.info("{}", metrics); // JSON 格式输出（Jackson toString 序列化）
        } catch (Exception e) {
            monitorLog.warn("监控采集异常", e);
        }
    }
}
