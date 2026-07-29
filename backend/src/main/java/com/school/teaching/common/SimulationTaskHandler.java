package com.school.teaching.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 仿真任务处理器 — 覆盖 SIMULATION 任务类型。
 *
 * 核心流程：加载提交记录 → 保存操作录制(events/duration/score) → 更新提交状态。
 */
@Component
@RequiredArgsConstructor
public class SimulationTaskHandler implements TaskTypeHandler {

    private final SimulationTaskMapper simTaskMapper;
    private final SimulationRecordingMapper recordingMapper;
    private final TaskSubmissionMapper submissionMapper;

    @Override
    public Set<TaskCategory> getCategories() {
        return Set.of(TaskCategory.SIMULATION);
    }

    @Override
    @Transactional
    public TaskSubmission onSubmit(TaskContext ctx) {
        Long submissionId = ctx.extras() != null ? (Long) ctx.extras().get("submissionId") : null;
        TaskSubmission sub = submissionId != null ? submissionMapper.selectById(submissionId) : null;
        if (sub == null) throw new BusinessException(404, "提交记录不存在");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) ctx.submission().get("payload");
        if (payload == null) throw new BusinessException(400, "缺少提交数据");

        // 保存操作录制
        SimulationRecording rec = new SimulationRecording();
        rec.setSubmissionId(sub.getId());
        rec.setStudentId(ctx.studentId());
        if (payload.get("events") != null) {
            try {
                rec.setEventsJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload.get("events")));
            } catch (Exception ignored) {
                rec.setEventsJson(payload.get("events").toString());
            }
        }
        rec.setDurationSeconds(payload.get("durationSeconds") instanceof Number ? ((Number) payload.get("durationSeconds")).intValue() : 0);
        rec.setSuccess(payload.get("success") instanceof Boolean && (Boolean) payload.get("success") ? 1 : 0);
        rec.setAutoScore(payload.get("autoScore") instanceof Number ? BigDecimal.valueOf(((Number) payload.get("autoScore")).doubleValue()) : BigDecimal.ZERO);
        rec.setSchoolId(ctx.extras() != null ? (Long) ctx.extras().get("schoolId") : 1L);
        recordingMapper.insert(rec);

        // 更新提交
        sub.setScore(rec.getAutoScore());
        sub.setStatus("SUBMITTED");
        sub.setSubmittedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);
        return sub;
    }
}
