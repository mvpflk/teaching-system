package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TaskCheatService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskCheatServiceImpl implements TaskCheatService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final CheatEventLogMapper cheatEventLogMapper;

    private static final Logger log = LoggerFactory.getLogger(TaskCheatServiceImpl.class);

    @Override
    @Transactional
    public Map<String, Object> recordCheatWarning(Long taskId, Long studentId, String eventType, boolean syncOnly) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        Map<String, Object> config = parseConfig(task.getTaskConfig());
        int maxWarnings = 3;
        if (config.containsKey("maxCheatWarnings")) {
            maxWarnings = Integer.parseInt(config.get("maxCheatWarnings").toString());
        } else if (config.containsKey("maxWarnings")) {
            maxWarnings = Integer.parseInt(config.get("maxWarnings").toString());
        } else if (config.containsKey("max_cheat_warnings")) {
            maxWarnings = Integer.parseInt(config.get("max_cheat_warnings").toString());
        } else {
            try {
                var globalSetting = systemSettingMapper.selectOne(
                    new LambdaQueryWrapper<SystemSetting>()
                        .eq(SystemSetting::getSettingKey, "exam.max_cheat_warnings"));
                if (globalSetting != null && globalSetting.getSettingValue() != null) {
                    maxWarnings = Integer.parseInt(globalSetting.getSettingValue());
                }
            } catch (Exception ignored) { log.debug("读取作弊警告配置失败: {}", ignored.getMessage()); }
        }
        if (maxWarnings <= 0) {
            return Map.of("cheatWarnings", 0, "maxCheatWarnings", 0, "terminated", false);
        }

        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            return Map.of("cheatWarnings", 0, "maxCheatWarnings", maxWarnings, "terminated", false,
                "message", "任务已截止");
        }

        TaskSubmission pendingSub = submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, taskId)
            .eq(TaskSubmission::getStudentId, studentId)
            .eq(TaskSubmission::getStatus, "PENDING")
            .orderByDesc(TaskSubmission::getId)
            .last("LIMIT 1"));
        if (pendingSub == null) {
            TaskSubmission anySub = submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .orderByDesc(TaskSubmission::getCreatedAt)
                .last("LIMIT 1"));
            boolean isTerminated = anySub != null && "TERMINATED".equals(anySub.getStatus());
            return Map.of("cheatWarnings", anySub != null ? (anySub.getCheatWarnings() != null ? anySub.getCheatWarnings() : 0) : 0,
                "maxCheatWarnings", maxWarnings, "terminated", isTerminated);
        }

        if (syncOnly) {
            int currentWarnings = pendingSub.getCheatWarnings() != null ? pendingSub.getCheatWarnings() : 0;
            boolean isTerminated = pendingSub.getCheatTerminated() != null && pendingSub.getCheatTerminated() == 1;
            return Map.of("cheatWarnings", currentWarnings,
                "maxCheatWarnings", maxWarnings, "terminated", isTerminated);
        }

        // 考试启动宽限期：刚创建的 PENDING 5 秒内不计数
        // 前端全屏初始化（requestFullscreen 重复调用）可能触发 visibility/fullscreen 变化误报
        if (pendingSub.getCreatedAt() != null) {
            long secondsSinceStart = java.time.Duration.between(
                pendingSub.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceStart < 10) {
                return Map.of("cheatWarnings",
                    pendingSub.getCheatWarnings() != null ? pendingSub.getCheatWarnings() : 0,
                    "maxCheatWarnings", maxWarnings, "terminated", false,
                    "gracePeriod", true);
            }
        }

        if ("FORMATIVE".equals(task.getTaskType()) || "SUMMATIVE".equals(task.getTaskType())) {
            int durationMinutes = getDurationMinutes(task);
            if (durationMinutes > 0 && pendingSub.getCreatedAt() != null
                && LocalDateTime.now().isAfter(pendingSub.getCreatedAt().plusMinutes(durationMinutes))) {
                submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getId, pendingSub.getId())
                    .set(TaskSubmission::getStatus, "TERMINATED")
                    .set(TaskSubmission::getCheatTerminated, 0)
                    .set(TaskSubmission::getScore, BigDecimal.ZERO)
                    .set(TaskSubmission::getSubmittedAt, LocalDateTime.now())
                    .set(TaskSubmission::getGradedAt, LocalDateTime.now())
                    .set(TaskSubmission::getGradingMessage,
                        "考试时间到（限时" + durationMinutes + "分钟），系统自动终止答题并记为0分。"));
                return Map.of("cheatWarnings", pendingSub.getCheatWarnings() != null ? pendingSub.getCheatWarnings() : 0,
                    "maxCheatWarnings", maxWarnings, "terminated", true,
                    "message", "考试时间已到，答题已自动终止");
            }
        }

        LambdaUpdateWrapper<TaskSubmission> uw = new LambdaUpdateWrapper<TaskSubmission>()
            .eq(TaskSubmission::getId, pendingSub.getId())
            .eq(TaskSubmission::getStatus, "PENDING")
            .setSql("cheat_warnings = COALESCE(cheat_warnings, 0) + 1");
        int affected = submissionMapper.update(null, uw);
        if (affected == 0) throw new BusinessException(404, "未找到进行中的提交记录");

        TaskSubmission sub = submissionMapper.selectById(pendingSub.getId());
        int warnings = sub.getCheatWarnings() == null ? 0 : sub.getCheatWarnings();
        boolean terminated = warnings >= maxWarnings;

        if (terminated && (sub.getCheatTerminated() == null || sub.getCheatTerminated() == 0)) {
            LambdaUpdateWrapper<TaskSubmission> terminate = new LambdaUpdateWrapper<TaskSubmission>()
                .eq(TaskSubmission::getId, sub.getId())
                .set(TaskSubmission::getCheatTerminated, 1)
                .set(TaskSubmission::getStatus, "TERMINATED")
                .set(TaskSubmission::getScore, BigDecimal.ZERO)
                .set(TaskSubmission::getSubmittedAt, LocalDateTime.now())
                .set(TaskSubmission::getGradedAt, LocalDateTime.now())
                .set(TaskSubmission::getGradingMessage,
                    "考试期间切屏 " + warnings + " 次(上限" + maxWarnings + "次)，系统自动终止答题并记为0分。如有疑问请联系任课教师。");
            submissionMapper.update(null, terminate);
        }

        try {
            CheatEventLog eventLog = new CheatEventLog();
            eventLog.setTaskId(taskId);
            eventLog.setStudentId(studentId);
            eventLog.setSubmissionId(sub != null ? sub.getId() : null);
            eventLog.setEventType(eventType != null ? eventType : "UNKNOWN");
            eventLog.setCheatWarnings(warnings);
            eventLog.setMaxWarnings(maxWarnings);
            cheatEventLogMapper.insert(eventLog);
        } catch (Exception e) {
            log.warn("切屏审计日志写入失败(taskId={}, studentId={}): {}", taskId, studentId, e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cheatWarnings", warnings);
        result.put("maxCheatWarnings", maxWarnings);
        result.put("terminated", terminated);
        return result;
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return JsonUtils.parseMap(json);
    }

    private int getDurationMinutes(Task task) {
        try {
            Map<String, Object> cfg = parseConfig(task.getTaskConfig());
            if (cfg.containsKey("durationMinutes")) {
                return Integer.parseInt(cfg.get("durationMinutes").toString());
            }
        } catch (Exception ignored) { log.debug("解析任务时长配置失败: {}", ignored.getMessage()); }
        return 0;
    }
}
