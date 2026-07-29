package com.school.teaching.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class DiagramUploader {

    private static final Logger log = LoggerFactory.getLogger(DiagramUploader.class);

    @Value("${teaching.upload-dir:/data/uploads}")
    private String baseDir;

    public String upload(String svgContent, Long teacherId) {
        try {
            Path dir = Paths.get(baseDir, "diagrams");
            Files.createDirectories(dir);
            String filename = "geom_" + UUID.randomUUID().toString().substring(0, 8) + ".svg";
            Path target = dir.resolve(filename);
            Files.writeString(target, svgContent, StandardCharsets.UTF_8);
            log.info("SVG 已写入: {} (teacherId={})", target, teacherId);
            return "/api/uploads/diagrams/" + filename;
        } catch (IOException e) {
            log.warn("SVG 文件写入失败: teacherId={}", teacherId, e);
            return null;
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpired() {
        Path dir = Paths.get(baseDir, "diagrams");
        if (!Files.exists(dir)) return;
        long cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        try (var files = Files.list(dir)) {
            files.filter(p -> p.toString().endsWith(".svg"))
                .filter(p -> p.toFile().lastModified() < cutoff)
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) { log.debug("SVG文件清理失败", ignored); }
                });
            log.info("SVG 过期清理完成: dir={}", dir);
        } catch (IOException e) {
            log.warn("SVG 过期清理失败", e);
        }
    }
}
