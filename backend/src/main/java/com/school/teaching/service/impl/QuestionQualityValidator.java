package com.school.teaching.service.impl;

import com.school.teaching.entity.QuestionBank;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 题目质量规则引擎 — 入库前对 AI 生成题目做确定性校验。
 *
 * <p>检查项：
 * <ul>
 *   <li>选项互斥性：单/多选题选项间是否存在包含或等价关系</li>
 *   <li>答案有效性：答案字母是否指向有效选项</li>
 *   <li>答案非空：correctAnswer 不得为空</li>
 *   <li>题干完整性：题干不得为空或过短</li>
 * </ul>
 *
 * <p>零 AI 调用，纯规则运算。发现问题时返回描述列表，
 * 调用方决定是标记待审核还是拒绝入库。
 *
 * @since V055-fix
 */
@Slf4j
public class QuestionQualityValidator {

    /** 单条质量问题 */
    public record Issue(String field, String message) {}

    /**
     * 对一道题目执行全量规则检查。
     *
     * @param qb 已归一化的题目实体（options/answer 已落定）
     * @return 问题列表，空 = 全部通过
     */
    public static List<Issue> validate(QuestionBank qb) {
        List<Issue> issues = new ArrayList<>();
        String qt = qb.getQuestionType();

        // ── 1. 选项互斥性（SINGLE_CHOICE / MULTI_CHOICE） ──
        if ("SINGLE_CHOICE".equals(qt) || "MULTI_CHOICE".equals(qt)) {
            checkOptionExclusion(qb, issues);
        }

        // ── 2. 答案有效性 ──
        checkAnswerValidity(qb, issues);

        // ── 3. 题干完整性 ──
        checkStemCompleteness(qb, issues);

        // ── 4. 选项格式兼容性 ── 检测 [{"key":"A","text":"..."}] 对象格式
        checkOptionsFormat(qb, issues);

        return issues;
    }

    /**
     * 检查选项格式：前端 parseOpts 优先接收字符串数组，
     * [{"key":"A","text":"..."}] 对象格式应在入库时归一化。
     */
    private static void checkOptionsFormat(QuestionBank qb, List<Issue> issues) {
        String optsJson = qb.getOptions();
        if (optsJson == null || optsJson.isBlank()) return;
        try {
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(optsJson, Object.class);
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                boolean hasObject = list.stream().anyMatch(e -> e instanceof Map);
                if (hasObject) {
                    issues.add(new Issue("options",
                        "选项格式为对象数组 [{\"key\":...,\"text\":...}]，"
                        + "建议归一化为字符串数组 [\"A. 文本\", ...] 以保证前端兼容"));
                }
            }
        } catch (Exception e) {
            issues.add(new Issue("options",
                "选项JSON无法解析: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  选项互斥性：检测包含关系和完全重复
    // ═══════════════════════════════════════════════════════

    private static void checkOptionExclusion(QuestionBank qb, List<Issue> issues) {
        List<String> options = parseOptions(qb.getOptions());
        if (options.size() < 2) return;

        // 归一化：去首尾空白 + 去标点 + 小写
        List<String> normalized = options.stream()
            .map(QuestionQualityValidator::normalizeOptionText)
            .toList();

        for (int i = 0; i < normalized.size(); i++) {
            for (int j = i + 1; j < normalized.size(); j++) {
                String a = normalized.get(i);
                String b = normalized.get(j);
                if (a.isEmpty() || b.isEmpty()) continue;

                if (a.equals(b)) {
                    issues.add(new Issue("options",
                        String.format("选项%s与%s内容完全相同: \"%s\"",
                            (char) ('A' + i), (char) ('A' + j), options.get(i))));
                } else if (a.contains(b) || b.contains(a)) {
                    issues.add(new Issue("options",
                        String.format("选项%s与%s存在包含关系: \"%s\" / \"%s\"",
                            (char) ('A' + i), (char) ('A' + j),
                            options.get(i), options.get(j))));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  答案有效性：字母范围 + 非空
    // ═══════════════════════════════════════════════════════

    private static void checkAnswerValidity(QuestionBank qb, List<Issue> issues) {
        String answer = qb.getCorrectAnswer();
        if (answer == null || answer.isBlank()) {
            issues.add(new Issue("correctAnswer", "正确答案为空"));
            return;
        }

        List<String> options = parseOptions(qb.getOptions());
        if (options.isEmpty()) return; // 填空题等无选项

        int optCount = options.size();
        String cleanAnswer = answer.trim().toUpperCase().replaceAll("[^A-Z]", "");

        if (cleanAnswer.isEmpty()) {
            issues.add(new Issue("correctAnswer",
                "答案无法解析为有效字母: \"" + answer + "\""));
            return;
        }

        char maxLetter = (char) ('A' + optCount - 1);
        for (char c : cleanAnswer.toCharArray()) {
            if (c > maxLetter) {
                issues.add(new Issue("correctAnswer",
                    String.format("答案字母 %c 超出选项范围(最大 %c, 共%d个选项)",
                        c, maxLetter, optCount)));
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  题干完整性
    // ═══════════════════════════════════════════════════════

    private static void checkStemCompleteness(QuestionBank qb, List<Issue> issues) {
        String text = qb.getQuestionText();
        if (text == null || text.isBlank()) {
            issues.add(new Issue("questionText", "题干为空"));
        } else if (text.trim().length() < 5) {
            issues.add(new Issue("questionText",
                "题干过短(" + text.trim().length() + "字): \"" + text.trim() + "\""));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════

    /** 解析 options JSON 字符串为文本列表 */
    private static List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank() || "[]".equals(optionsJson.trim())) {
            return Collections.emptyList();
        }
        try {
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(optionsJson, Object.class);
            if (parsed instanceof List<?> list) {
                List<String> result = new ArrayList<>();
                for (Object item : list) {
                    result.add(item != null ? String.valueOf(item) : "");
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("选项JSON解析失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /** 归一化选项文本：去空白、去标点、小写 */
    private static String normalizeOptionText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s ]+", "")
            .replaceAll("[，,。．.；;：:！!？?、（）()【】\\[\\]《》\"\"''·]", "")
            .toLowerCase()
            .trim();
    }
}
