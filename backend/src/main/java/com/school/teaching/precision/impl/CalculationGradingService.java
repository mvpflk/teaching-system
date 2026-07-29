package com.school.teaching.precision.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.service.impl.DeepSeekGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解答题（CALCULATION / PROOF）AI 评分服务。
 *
 * 三级置信度体系（与 OCR Vision 评分一致）：
 *   ≥ 0.85 → 自动判分，直接采纳
 *   0.70 ~ 0.85 → AI 建议分数，标记"建议复核"，教师最终确认
 *   < 0.70 → 标记"需教师评阅"，AI 不判分
 *
 * 评分维度（总分 100，映射到诊断百分制）：
 *   解题思路正确性 40% — 方法选择是否正确
 *   关键步骤完整性 30% — 推导过程是否完整
 *   最终答案正确性 30% — 结果是否正确
 */
@Slf4j
@Service
public class CalculationGradingService {

    @Autowired(required = false)
    private DeepSeekGateway deepSeekGateway;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 置信度阈值：达到此值自动判分 */
    private static final double AUTO_CONFIDENCE = 0.85;
    /** 置信度阈值：低于此值完全交由教师 */
    private static final double TEACHER_CONFIDENCE = 0.70;

    /**
     * 对解答题答案进行 AI 评分。
     *
     * @param question      题目（含 questionText / correctAnswer / explanation）
     * @param studentAnswer 学生作答文本
     * @param maxScore      满分值（诊断=100，考试=实际分值）
     * @return 评分结果:
     *   matchMode: "ai_graded" | "ai_suggested" | "pending_review" | "ai_unavailable"
     *   score / maxScore / confidence / feedback / thoughtScore / stepScore / answerScore
     */
    public Map<String, Object> grade(QuestionBank question, String studentAnswer, int maxScore) {
        // 空答案直接标记
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchMode", "unanswered");
            result.put("score", 0);
            result.put("maxScore", maxScore);
            result.put("feedback", "未作答");
            return result;
        }

        // 答案太短（< 5 字符）→ 标记未认真作答
        if (studentAnswer.trim().length() < 5) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchMode", "unanswered");
            result.put("score", 0);
            result.put("maxScore", maxScore);
            result.put("feedback", "请认真作答（至少写清楚解题过程和答案）。");
            return result;
        }

        // AI 不可用 → 降级为教师评阅
        if (deepSeekGateway == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchMode", "pending_review");
            result.put("score", 0);
            result.put("maxScore", maxScore);
            result.put("feedback", "AI评分服务未配置，已提交教师评阅");
            result.put("confidence", 0.0);
            return result;
        }

        // 构建评分请求
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("questionText", buildGradingPrompt(question));
            params.put("studentAnswer", studentAnswer);
            params.put("referenceAnswer", question.getCorrectAnswer());
            params.put("maxScore", maxScore);

            Map<String, Object> aiResult = deepSeekGateway.scoreTextAnswer(params);

            // 提取 AI 返回的评分数据
            int score = toInt(aiResult.get("score"), 0);
            double confidence = toDouble(aiResult.get("confidence"), 0.0);
            String comment = String.valueOf(aiResult.getOrDefault("comment", ""));
            String explanation = String.valueOf(aiResult.getOrDefault("explanation", ""));
            int tokensUsed = toInt(aiResult.get("_tokensUsed"), 0);

            // 钳制分数范围
            score = Math.max(0, Math.min(maxScore, score));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("score", score);
            result.put("maxScore", maxScore);
            result.put("confidence", Math.round(confidence * 100) / 100.0);
            result.put("feedback", explanation.isEmpty() ? comment : explanation);
            result.put("tokensUsed", tokensUsed);

            // 按置信度分档
            if (confidence >= AUTO_CONFIDENCE) {
                result.put("matchMode", "ai_graded");
                result.put("isCorrect", score >= maxScore * 0.6); // 60% 以上算通过
            } else if (confidence >= TEACHER_CONFIDENCE) {
                result.put("matchMode", "ai_suggested");
                result.put("isCorrect", null); // 不确定，等教师确认
                result.put("suggestionNote", "AI已给出建议分数，请教师复核确认");
            } else {
                result.put("matchMode", "pending_review");
                result.put("isCorrect", null);
                result.put("feedback", "AI评阅置信度较低(" + String.format("%.0f", confidence * 100)
                    + "%)，已提交教师评阅。AI参考: " + explanation);
            }

            return result;

        } catch (BusinessException e) {
            // AI 调用失败 → 降级为教师评阅
            log.warn("AI评分调用失败(qid={}): {}", question.getId(), e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchMode", "pending_review");
            result.put("score", 0);
            result.put("maxScore", maxScore);
            result.put("feedback", "AI评分暂时不可用(" + e.getMessage() + ")，已提交教师评阅");
            result.put("confidence", 0.0);
            return result;
        } catch (Exception e) {
            log.error("AI评分异常(qid={}): {}", question.getId(), e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchMode", "pending_review");
            result.put("score", 0);
            result.put("maxScore", maxScore);
            result.put("feedback", "评分系统异常，已提交教师评阅");
            result.put("confidence", 0.0);
            return result;
        }
    }

    /**
     * 构建发送给 AI 的评分 Prompt。
     * 包含题目正文 + 参考解析（作为评分依据）+ 评分维度说明。
     */
    private String buildGradingPrompt(QuestionBank question) {
        StringBuilder sb = new StringBuilder();
        sb.append("【题目】").append(question.getQuestionText() != null ? question.getQuestionText() : "");

        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            sb.append("\n【参考解析与评分标准】").append(question.getExplanation());
        }

        sb.append("\n【评分要求】按以下维度评分：");
        sb.append("\n1. 解题思路正确性(40%)—方法选择是否正确、逻辑是否清晰");
        sb.append("\n2. 关键步骤完整性(30%)—推导过程是否完整、有无跳步");
        sb.append("\n3. 最终答案正确性(30%)—结果是否正确、表达是否规范");
        sb.append("\n\n注意：");
        sb.append("\n- 学生使用与参考答案不同的合法方法也应给分");
        sb.append("\n- 计算错误但思路正确应给部分分（至少给思路分）");
        sb.append("\n- 答案形式不同但数值等价应视为正确（如 1/2 和 0.5）");

        return sb.toString();
    }

    private int toInt(Object val, int defaultVal) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private double toDouble(Object val, double defaultVal) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }
}
