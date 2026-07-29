package com.school.teaching.agent.audit;

import com.school.teaching.agent.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AgentAuditLogger {

    private static final int RETENTION_DAYS = 90;

    private final JdbcTemplate jdbc;

    public AgentAuditLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String sessionId, UserContext caller, String toolName, String toolArgs,
                    boolean accessGranted, String denyReason, long executionTimeMs, String resultSummary) {
        try {
            jdbc.update(
                    "INSERT INTO agent_tool_call_log (session_id, user_id, role_name, tool_name, tool_args, " +
                            "access_granted, deny_reason, execution_time_ms, result_summary, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    sessionId, caller.getUserId(), caller.getRoleName(), toolName, toolArgs,
                    accessGranted ? 1 : 0, denyReason, executionTimeMs, resultSummary, LocalDateTime.now()
            );
        } catch (Exception e) {
            log.warn("审计日志写入失败: tool={}, userId={}", toolName, caller.getUserId(), e);
        }
    }

    /**
     * 每天凌晨 3 点清理超过 90 天的审计日志。
     */
    @Scheduled(cron = "0 7 3 * * *")
    public void cleanupExpiredLogs() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int deleted = jdbc.update("DELETE FROM agent_tool_call_log WHERE created_at < ?", cutoff);
            if (deleted > 0) {
                log.info("审计日志清理完成: 删除 {} 条（保留 {} 天）", deleted, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.warn("审计日志清理失败", e);
        }
    }
}
