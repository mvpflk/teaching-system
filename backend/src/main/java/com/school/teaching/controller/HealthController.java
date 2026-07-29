package com.school.teaching.controller;

import com.school.teaching.common.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private List<HealthIndicator> healthIndicators;

    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());

        // 基本检查：数据库连通性
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("db", checkDb());
        checks.put("memory", checkMemory());
        status.put("checks", checks);

        boolean anyDown = checks.values().stream().anyMatch(m -> {
            Object s = ((Map<?, ?>) m).get("status");
            return "DOWN".equals(s);
        });
        if (anyDown) {
            return new R<>(503, "Service Unavailable", status, System.currentTimeMillis());
        }
        return R.ok(status);
    }

    private Map<String, Object> checkDb() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                db.put("status", "UP");
            } else {
                db.put("status", "UP");
                db.put("note", "no datasource");
            }
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> checkMemory() {
        Map<String, Object> mem = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        double pct = max > 0 ? (double) used / max * 100 : 0;
        mem.put("status", pct < 95 ? "UP" : "DEGRADED");
        mem.put("usedPercent", Math.round(pct * 100.0) / 100.0);
        mem.put("usedMB", used / (1024 * 1024));
        mem.put("maxMB", max / (1024 * 1024));
        return mem;
    }
}
