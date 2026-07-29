package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.AiCallLog;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.AiCallLogMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.metrics.AiMetricsService;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiServiceGateway;
import com.school.teaching.service.SystemService;
import com.school.teaching.agent.prompt.PromptTemplateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemedialExerciseService {

    private final AiServiceGateway aiGateway;
    private final KnowledgeNodeMapper nodeMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiMetricsService aiMetrics;
    private final QuestionSaveService questionSaveService;
    private final SystemService systemService;
    private final PromptTemplateCache promptTemplateCache;

    public Map<String, Object> generateRemedial(Long userId, List<Integer> knowledgeNodeIds, String subject) {
        int quota = systemService.getIntConfig("ai.student.daily_quota", 6);
        long todayCount = 0;
        if (quota > 0) {
            todayCount = aiCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiCallLog>()
                    .eq(AiCallLog::getUserId, userId)
                    .eq(AiCallLog::getCapability, "REMEDIAL")
                    .ge(AiCallLog::getCreatedAt, LocalDate.now().atStartOfDay()));
            if (todayCount >= quota) throw new BusinessException(429, "今日练习次数已用完（" + quota + "次/天）");
        }
        List<String> nodeNames = new ArrayList<>();
        if (knowledgeNodeIds != null && !knowledgeNodeIds.isEmpty()) {
            List<Long> longIds = knowledgeNodeIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
            List<KnowledgeNode> nodes = nodeMapper.selectBatchIds(longIds);
            if (nodes != null) {
                for (KnowledgeNode n : nodes) {
                    if (n != null) nodeNames.add(n.getName());
                }
            }
        }
        String topPoint = nodeNames.isEmpty() ? "薄弱知识点" : nodeNames.get(0);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("knowledgePoint", topPoint);
        StringBuilder ref = new StringBuilder("以下是学生的薄弱知识点分析，请针对这些薄弱点生成针对性练习题：\n");
        for (int i = 0; i < nodeNames.size(); i++) {
            ref.append(i + 1).append(". ").append(nodeNames.get(i)).append("\n");
        }
        if (subject != null && !subject.isEmpty()) ref.append("学科：").append(subject).append("\n");
        ref.append("请生成10道题，错误次数多的知识点应分配更多题目。题型需包含单选、多选、判断、填空。");
        params.put("referenceMaterial", ref.toString());
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        typeCounts.put("SINGLE_CHOICE", 4);
        typeCounts.put("MULTI_CHOICE", 2);
        typeCounts.put("TRUE_FALSE", 2);
        typeCounts.put("FILL_IN", 2);
        params.put("typeCounts", typeCounts);
        params.put("candidateCount", 10);
        if (subject != null) params.put("subject", subject);
        params.putIfAbsent("stageHint", "");
        params.put("difficultyLevel", 2);
        if (knowledgeNodeIds != null && !knowledgeNodeIds.isEmpty()) {
            params.put("categoryId", Long.valueOf(knowledgeNodeIds.get(0)));
        }
        params.put("_instructionPrompt", buildRemedialPrompt(knowledgeNodeIds, nodeNames,
            subject, params.get("typeCounts")));
        params.put("_skipGenericFormat", Boolean.TRUE);
        long start = System.currentTimeMillis();
        List<Map<String, Object>> questions = aiGateway.generateQuestions(params);
        List<Map<String, Object>> saved = questionSaveService.saveQuestions(userId, params, questions);
        int latency = (int) (System.currentTimeMillis() - start);
        AiCallLog callLog = new AiCallLog();
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        callLog.setSchoolId(schoolId != null ? schoolId : 1L);
        callLog.setUserId(userId);
        callLog.setCapability("REMEDIAL");
        callLog.setProvider(aiGateway.getProvider());
        callLog.setTokensUsed(0);
        callLog.setLatencyMs(latency);
        callLog.setStatus("SUCCESS");
        aiCallLogMapper.insert(callLog);
        aiMetrics.recordCall("REMEDIAL", "SUCCESS", 0, 0, 0, latency, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", saved);
        result.put("quotaRemaining", quota > 0 ? quota - (int) todayCount - 1 : -1);
        return result;
    }

    private String buildRemedialPrompt(List<Integer> knowledgeNodeIds,
            List<String> nodeNames, String subject, Object typeCountsObj) {
        String bareSubject = subject != null ? subject.replaceAll("\\[.*?\\]", "").trim() : "通用";
        StringBuilder sb = new StringBuilder();
        sb.append("你是中职").append(bareSubject).append("教师。");
        sb.append("请根据以下学生薄弱知识点分析，生成针对性练习题。\n\n");
        sb.append("【薄弱知识点】\n");
        for (int i = 0; i < nodeNames.size(); i++) {
            sb.append(i + 1).append(". ").append(nodeNames.get(i));
            if (knowledgeNodeIds != null && i < knowledgeNodeIds.size())
                sb.append(" (ID:").append(knowledgeNodeIds.get(i)).append(")");
            sb.append("\n");
        }
        sb.append("\n【出题策略】错误次数多的知识点应分配更多题目。\n");
        if (typeCountsObj instanceof Map<?, ?> tc && !tc.isEmpty()) {
            sb.append("【题型分布】");
            tc.forEach((k, v) -> sb.append(typeLabelRemedial(String.valueOf(k))).append("×").append(v).append(" "));
            sb.append("\n");
        }
        sb.append("\n【输出要求】\n");
        sb.append("1. 每题含 questionText/questionType/options/correctAnswer/explanation/difficultyLevel/knowledgeNodeId\n");
        sb.append("2. explanation 须指出错误选项错因+正确解法\n");
        sb.append("3. 选项为4个，干扰项基于典型错误认知\n");
        sb.append("4. 纯JSON数组输出\n");
        sb.append("\n【命题质量控制——必须严格遵守】\n");
        sb.append("1. 选项唯一性：同一道题的所有选项内容必须互不相同。\n");
        sb.append("2. 题干差异性：禁止生成多道题干高度相似的题目。\n");
        sb.append("3. 答案字母精确：correctAnswer的字母必须严格对应选项数组的索引位置。\n");
        sb.append("4. 生成前自检：确认选项无重复、答案字母未越界后再输出。\n");
        if (promptTemplateCache != null) {
            String override = promptTemplateCache.getFinal("remedial_exercise", bareSubject);
            if (override != null) return override;
        }
        return sb.toString();
    }

    private static String typeLabelRemedial(String key) {
        return switch (key) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTI_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_IN" -> "填空题";
            case "SHORT_ANSWER", "ESSAY" -> "简答题";
            case "CALCULATION" -> "计算题";
            default -> "题目";
        };
    }
}
