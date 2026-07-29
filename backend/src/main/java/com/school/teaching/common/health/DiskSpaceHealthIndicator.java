package com.school.teaching.common.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 磁盘空间健康检查。
 * 检查上传目录所在分区的可用空间，低于 1GB 警告，低于 200MB 降级。
 */
@Component
public class DiskSpaceHealthIndicator implements HealthIndicator {

    @Value("${teaching.upload-dir:uploads}")
    private String uploadDir;

    private static final long WARN_THRESHOLD = 1024L * 1024 * 1024;   // 1 GB
    private static final long DOWN_THRESHOLD = 200L * 1024 * 1024;    // 200 MB

    @Override
    public Health health() {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            long free = dir.getFreeSpace();
            long total = dir.getTotalSpace();
            double freeGB = free / (1024.0 * 1024 * 1024);
            double totalGB = total / (1024.0 * 1024 * 1024);

            if (free < DOWN_THRESHOLD) {
                return Health.down()
                        .withDetail("path", dir.getAbsolutePath())
                        .withDetail("freeGB", String.format("%.1f", freeGB))
                        .withDetail("totalGB", String.format("%.1f", totalGB))
                        .withDetail("error", "可用空间不足 200MB，上传功能可能失败")
                        .build();
            }
            if (free < WARN_THRESHOLD) {
                return Health.status("WARN")
                        .withDetail("path", dir.getAbsolutePath())
                        .withDetail("freeGB", String.format("%.1f", freeGB))
                        .withDetail("totalGB", String.format("%.1f", totalGB))
                        .withDetail("warning", "可用空间不足 1GB，建议清理")
                        .build();
            }
            return Health.up()
                    .withDetail("path", dir.getAbsolutePath())
                    .withDetail("freeGB", String.format("%.1f", freeGB))
                    .withDetail("totalGB", String.format("%.1f", totalGB))
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }
}
