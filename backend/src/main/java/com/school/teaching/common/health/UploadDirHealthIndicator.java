package com.school.teaching.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;

/**
 * 上传目录可写性检查。
 * 在目录下创建临时文件，验证可写权限后立即删除。
 */
@Slf4j
@Component
public class UploadDirHealthIndicator implements HealthIndicator {

    @Value("${teaching.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public Health health() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                return Health.down()
                        .withDetail("path", dir.getAbsolutePath())
                        .withDetail("error", "上传目录不存在且无法创建")
                        .build();
            }
        }

        File testFile = new File(dir, ".health-check-" + System.currentTimeMillis() + ".tmp");
        try {
            // 写入测试
            try (FileWriter fw = new FileWriter(testFile)) {
                fw.write("ok");
            }
            // 读取验证
            if (!testFile.exists() || testFile.length() == 0) {
                return Health.down()
                        .withDetail("path", dir.getAbsolutePath())
                        .withDetail("error", "测试文件写入后不可读")
                        .build();
            }
            // 删除测试
            boolean deleted = testFile.delete();
            return Health.up()
                    .withDetail("path", dir.getAbsolutePath())
                    .withDetail("writable", true)
                    .withDetail("cleanupOk", deleted)
                    .build();

        } catch (Exception e) {
            // 清理残留
            try { testFile.delete(); } catch (Exception ignored) { log.debug("临时文件清理失败", ignored); }
            return Health.down()
                    .withDetail("path", dir.getAbsolutePath())
                    .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }
}
