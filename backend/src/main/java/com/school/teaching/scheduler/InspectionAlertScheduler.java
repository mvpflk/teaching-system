package com.school.teaching.scheduler;

import com.school.teaching.service.InspectionAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InspectionAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(InspectionAlertScheduler.class);

    @Autowired
    private InspectionAlertService inspectionAlertService;

    /** 每小时执行一次巡视预警检查（39分偏移，避开整点高峰） */
    @Async
    @Scheduled(cron = "0 39 * * * *")
    public void runCheck() {
        log.info("[巡视预警] 定时扫描开始");
        try {
            inspectionAlertService.checkAll();
            log.info("[巡视预警] 定时扫描完成");
        } catch (Exception e) {
            log.error("[巡视预警] 定时扫描失败", e);
        }
    }
}
