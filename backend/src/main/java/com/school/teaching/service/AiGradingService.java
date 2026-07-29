package com.school.teaching.service;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.metrics.AiMetricsService;
import com.school.teaching.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGradingService {

    private final AiServiceGateway aiGateway;
    private final QuestionBankMapper questionBankMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final AiTaskStore taskStore;
    private final AiMetricsService aiMetrics;

    @Value("${teaching.ai.grading.enabled:false}") private boolean aiGradingEnabled;

    /** 异步获取 AI 评分建议，结果写入 AiTaskStore */
    @Async("aiExecutor")
    public void executeAsync(String taskId, Long submissionId, Long questionId, Long teacherId) {
        taskStore.markRunning(taskId);
        try {
            if (!aiGradingEnabled) {
                taskStore.complete(taskId, Map.of("disabled", true));
                return;
            }

            TaskSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) throw new BusinessException(404, "提交不存在");
            QuestionBank q = questionBankMapper.selectById(questionId);
            if (q == null) throw new BusinessException(404, "题目不存在");

            long start = System.currentTimeMillis();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("questionText", q.getQuestionText());
            params.put("studentAnswer", sub.getContent() != null ? sub.getContent() : "(未作答)");
            params.put("referenceAnswer", q.getCorrectAnswer());
            params.put("maxScore", 10);

            Map<String, Object> aiResult = aiGateway.scoreTextAnswer(params);

            int tokens = aiResult.containsKey("_tokensUsed") ? ((Number) aiResult.remove("_tokensUsed")).intValue() : 0;
            int latency = (int) (System.currentTimeMillis() - start);

            AiCallLog callLog = new AiCallLog();
            callLog.setSchoolId(SecurityUtils.getCurrentSchoolId() != null ? SecurityUtils.getCurrentSchoolId() : 1L);
            callLog.setUserId(teacherId); callLog.setCapability("ESSAY_FEEDBACK");
            callLog.setProvider(aiGateway.getProvider()); callLog.setTokensUsed(tokens); callLog.setLatencyMs(latency);
            callLog.setStatus("SUCCESS");
            aiCallLogMapper.insert(callLog);
            aiMetrics.recordCall("ESSAY_FEEDBACK", "SUCCESS", tokens, 0, 0, latency, null);

            // null-safe 组装：AI 返回的 JSON 字段值可能为 null，Map.of 禁 null 会抛 NPE
            Map<String, Object> gradeResult = new java.util.HashMap<>();
            gradeResult.put("score", nvl(aiResult.get("score"), 0));
            gradeResult.put("comment", nvl(aiResult.get("comment"), ""));
            gradeResult.put("confidence", nvl(aiResult.get("confidence"), 0));
            gradeResult.put("explanation", nvl(aiResult.get("explanation"), ""));
            taskStore.complete(taskId, gradeResult);
        } catch (Exception e) {
            log.error("AI评分失败: taskId={}, submissionId={}", taskId, submissionId, e);
            aiMetrics.recordFailure("ESSAY_FEEDBACK", 0);
            taskStore.fail(taskId, e.getMessage());
        }
    }

    private static Object nvl(Object v, Object def) { return v != null ? v : def; }

    /** 管理员查看 AI 调用统计 */
    public Map<String, Object> getStats(Long schoolId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<AiCallLog> logs = aiCallLogMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getSchoolId, schoolId)
                .ge(AiCallLog::getCreatedAt, LocalDateTime.now().minusDays(30)));

        long totalCalls = logs.size();
        long totalTokens = logs.stream().mapToLong(l -> l.getTokensUsed() != null ? l.getTokensUsed() : 0).sum();
        double estimatedCost = totalTokens / 1000.0 * 0.002;
        Map<String, Long> byCapability = new HashMap<>();
        for (AiCallLog l : logs) {
            byCapability.merge(l.getCapability() != null ? l.getCapability() : "UNKNOWN", 1L, Long::sum);
        }
        stats.put("totalCalls", totalCalls); stats.put("totalTokens", totalTokens);
        stats.put("estimatedCost", String.format("$%.2f", estimatedCost));
        stats.put("byCapability", byCapability);
        return stats;
    }
}
