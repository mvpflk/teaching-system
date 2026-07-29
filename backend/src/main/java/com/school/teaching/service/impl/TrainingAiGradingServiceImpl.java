package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.service.impl.DeepSeekGateway;
import com.school.teaching.service.TrainingAiGradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实训 AI 评分实现 — 对非检查点类型步骤（text/file/sim/web）进行 AI 辅助评分
 */
@Slf4j
@Service
public class TrainingAiGradingServiceImpl implements TrainingAiGradingService {

    private final DeepSeekGateway aiGateway;
    private final ObjectMapper om = new ObjectMapper();

    public TrainingAiGradingServiceImpl(@Qualifier("deepSeekGateway") DeepSeekGateway aiGateway) {
        this.aiGateway = aiGateway;
    }

    @Override
    public Map<String, Object> gradeStep(String stepType, String stepDesc, int maxScore,
                                          Map<String, Object> studentData) {
        if (stepDesc == null || stepDesc.isBlank()) return null;
        if (studentData == null || studentData.isEmpty()) {
            return Map.of("score", 0, "reason", "学生未提交内容", "confidence", 1.0, "aiGraded", true);
        }

        String studentContent = formatStudentData(stepType, studentData);
        if (studentContent.isBlank()) {
            return Map.of("score", 0, "reason", "学生未提交内容", "confidence", 1.0, "aiGraded", true);
        }

        String prompt = buildGradingPrompt(stepType, stepDesc, maxScore, studentContent);

        // 最多 2 次尝试（含 1 次重试）
        for (int attempt = 0; attempt < 2; attempt++) {
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("prompt", prompt);
            params.put("maxTokens", 500);
            params.put("temperature", attempt == 0 ? 0.5 : 0.7); // 首次0.5，重试提温增加多样性

            String raw = aiGateway.generateContentQuiet(params);
            if (raw == null || raw.isBlank()) {
                if (attempt == 0) { log.info("AI评分空内容, 重试: stepType={}", stepType); continue; }
                log.warn("AI评分返回空(重试后): stepType={}", stepType);
                return null;
            }

            // 解析 JSON 响应
            Map<String, Object> result = om.readValue(extractJson(raw), Map.class);
            int score = result.get("score") instanceof Number n ? n.intValue() : 0;
            if (score < 0) score = 0;
            if (score > maxScore) score = maxScore;
            String reason = result.get("reason") instanceof String s ? s : "AI评分";
            double confidence = result.get("confidence") instanceof Number n ? n.doubleValue() : 0.7;
            if (confidence < 0) confidence = 0;
            if (confidence > 1) confidence = 1;

            Map<String, Object> graded = new LinkedHashMap<>();
            graded.put("score", score);
            graded.put("reason", reason);
            graded.put("confidence", confidence);
            graded.put("aiGraded", true);
            return graded;

        } catch (Exception e) {
            if (attempt == 0) {
                log.info("AI评分异常, 重试: stepType={}, err={}", stepType, e.getMessage());
                continue;
            }
            log.warn("AI评分失败(重试后回退手动): stepType={}, err={}", stepType, e.getMessage());
            return null;
        }
        } // end retry loop
        return null; // 不应到达
    }

    // ── 格式化学生提交内容 ──

    private String formatStudentData(String stepType, Map<String, Object> data) {
        return switch (stepType) {
            case "text" -> {
                Object content = data.get("content");
                yield content instanceof String s ? s : String.valueOf(content);
            }
            case "file" -> {
                Object files = data.get("files");
                if (files instanceof java.util.List<?> list && !list.isEmpty()) {
                    StringBuilder sb = new StringBuilder("已上传文件: ");
                    for (Object f : list) {
                        if (f instanceof Map<?, ?> fm) {
                            sb.append(fm.get("name")).append(", ");
                        }
                    }
                    yield sb.toString();
                }
                yield "";
            }
            case "sim" -> {
                Object completed = data.get("completed");
                yield Boolean.TRUE.equals(completed) ? "仿真操作已完成" : "仿真操作未完成";
            }
            case "web" -> {
                Object html = data.get("html");
                if (html instanceof String s && !s.isBlank()) yield s.substring(0, Math.min(s.length(), 2000));
                Object files = data.get("files");
                if (files instanceof java.util.List<?> list && !list.isEmpty()) {
                    yield "已上传 " + list.size() + " 个网页文件";
                }
                yield "";
            }
            default -> String.valueOf(data);
        };
    }

    // ── 构建评分 Prompt ──

    private String buildGradingPrompt(String stepType, String stepDesc, int maxScore, String studentContent) {
        String typeHint = switch (stepType) {
            case "text" -> "学生提交的是文字描述，请评估其是否完整回应了步骤要求";
            case "file" -> "学生上传了文件，请评估文件是否符合步骤要求";
            case "sim" -> "学生完成了仿真操作，请评估完成情况";
            case "web" -> "学生提交了网页代码或文件，请评估是否符合设计要求";
            default -> "请根据步骤要求评估学生提交内容";
        };

        return """
            你是计算机实训评分助手。根据步骤要求和学生提交内容进行评分。

            【步骤要求】%s
            【满分】%d 分
            【学生提交】%s
            【评分提示】%s

            请输出严格 JSON（不要包含其他文字）：
            {"score": 整数(0-%d), "reason": "评分理由(50字内，中文，发展性语言)", "confidence": 0.0-1.0}

            评分标准：
            - score=0: 完全不符合要求或未提交
            - score=满分*0.6: 基本符合但不完整
            - score=满分*0.8: 符合要求
            - score=满分: 完全符合且优秀
            - confidence<0.7: 需要教师复核
            """
            .formatted(stepDesc, maxScore, studentContent, typeHint, maxScore);
    }

    // ── 从 AI 响应中提取 JSON ──

    private String extractJson(String raw) {
        raw = raw.trim();
        // 去掉 markdown 代码块标记
        if (raw.startsWith("```")) {
            int end = raw.indexOf("\n");
            if (end > 0) raw = raw.substring(end + 1);
            if (raw.endsWith("```")) raw = raw.substring(0, raw.length() - 3);
        }
        raw = raw.trim();
        // 找到 { 和 }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            raw = raw.substring(start, end + 1);
        }
        return raw;
    }
}
