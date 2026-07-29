package com.school.teaching.scheduler;

import com.school.teaching.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScanScheduler.class);

    @Autowired private AlertService alertService;

    /** 增量扫描：每2小时（07分偏移），避开整点高峰 */
    @Async
    @Scheduled(cron = "0 7 */2 * * *")
    @SchedulerLock(name = "alert_incremental_scan", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void incrementalScan() {
        log.info("[学业预警] 增量扫描开始");
        try {
            int count = alertService.scanIncremental();
            log.info("[学业预警] 增量扫描完成，触发 {} 条预警", count);
        } catch (Exception e) {
            log.error("[学业预警] 增量扫描失败", e);
        }
    }

    /** 全量兜底：每天凌晨3:07 */
    @Async
    @Scheduled(cron = "0 7 3 * * *")
    @SchedulerLock(name = "alert_full_scan", lockAtMostFor = "30m", lockAtLeastFor = "1m")
    public void fullScan() {
        log.info("[学业预警] 全量扫描开始");
        try {
            int count = alertService.scanAllStudents();
            log.info("[学业预警] 全量扫描完成，触发 {} 条预警", count);
        } catch (Exception e) {
            log.error("[学业预警] 全量扫描失败", e);
        }
    }
}
