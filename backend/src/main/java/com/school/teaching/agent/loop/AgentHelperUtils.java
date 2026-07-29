package com.school.teaching.agent.loop;

import java.util.*;

/**
 * Agent 纯逻辑工具方法 —— 提取自 AgentLoopService，使核心推理逻辑可测试。
 * 所有方法均为 public static，无副作用，适合金标准回归测试。
 */
public final class AgentHelperUtils {

    private AgentHelperUtils() {}

    /** 检测用户消息是否包含多个独立任务 */
    public static boolean isMultiIntent(String msg) {
        if (msg == null || msg.isBlank()) return false;
        int patterns = 0;
        if (msg.contains("先") && (msg.contains("再") || msg.contains("然后") || msg.contains("接着"))) patterns++;
        if (msg.contains("同时") || msg.contains("另外") || msg.contains("还有") || msg.contains("并且")) patterns++;
        if (msg.contains("既要") && msg.contains("又要")) patterns++;
        int actionCount = 0;
        String[] actions = {"分析", "创建", "生成", "查询", "统计", "计算", "评估", "导出", "发送", "制定"};
        for (String a : actions) {
            if (msg.contains(a)) actionCount++;
        }
        if (actionCount >= 3) patterns++;
        return patterns >= 1;
    }

    /** 从用户消息中提取第一句作为原始目标 */
    public static String extractGoal(String msg) {
        if (msg == null || msg.isBlank()) return null;
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '!') {
                String goal = msg.substring(0, i).trim();
                return goal.length() > 3 ? goal : msg.substring(0, Math.min(60, msg.length()));
            }
        }
        return msg.length() > 60 ? msg.substring(0, 60) + "…" : msg;
    }

    /** 判断助理回复是否与原始目标相关 */
    public static boolean isOnTopic(String content, String goal) {
        if (content == null || goal == null) return true;
        Set<String> goalTerms = new HashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,}").matcher(goal);
        while (m.find()) { goalTerms.add(m.group()); }
        if (goalTerms.isEmpty()) return true;
        int matchCount = 0;
        for (String term : goalTerms) { if (content.contains(term)) matchCount++; }
        return matchCount >= Math.max(1, goalTerms.size() / 3);
    }

    /** 输出自反思——常见错误模式检测 */
    public static String selfReflect(String content) {
        if (content == null || content.isBlank()) return null;
        List<String> warnings = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        int answerIdx = 0;
        while ((answerIdx = content.indexOf("答案", answerIdx)) >= 0) {
            int colon = content.indexOf("：", answerIdx);
            if (colon < 0) colon = content.indexOf(":", answerIdx);
            if (colon < 0) colon = content.indexOf("是", answerIdx);
            if (colon > 0 && colon - answerIdx < 10) {
                int end = content.indexOf("\n", colon);
                if (end < 0) end = Math.min(colon + 30, content.length());
                String val = content.substring(colon + 1, end).trim().replaceAll("\\s+", "");
                if (!val.isEmpty()) answers.add(val);
            }
            answerIdx++;
        }
        if (answers.size() >= 2) {
            for (int i = 0; i < answers.size() - 1; i++) {
                String a = cleanAnswer(answers.get(i));
                String b = cleanAnswer(answers.get(i + 1));
                if (a.length() > 1 && b.length() > 1 && !answersEquivalent(a, b)) {
                    warnings.add("检测到多个不同答案值（\"" + a + "\" vs \"" + b + "\"），请核实一致性");
                    break;
                }
            }
        }
        for (String term : new String[]{"高中", "初中", "小学", "高考"}) {
            int idx = content.indexOf(term);
            if (idx < 0) continue;
            if (!isContextualComparison(content, idx, term)) {
                warnings.add("内容提及了非中职学段（" + term + "），如果受众为中职师生，建议调整表述");
                break;
            }
        }
        if (warnings.isEmpty()) return null;
        return "⚠️ 自检提示：" + String.join("；", warnings);
    }

    static String cleanAnswer(String raw) {
        return raw.replaceAll("[。，、；：；.!?,;:\\s（）()]", "")
                  .replaceAll("^[A-Da-d][.、．]?", "");
    }

    static boolean answersEquivalent(String a, String b) {
        if (a.equals(b)) return true;
        try {
            double na = Double.parseDouble(a);
            double nb = Double.parseDouble(b);
            return Math.abs(na - nb) < 0.0001;
        } catch (NumberFormatException ignored) {}
        return false;
    }

    static boolean isContextualComparison(String content, int pos, String term) {
        int radius = 80;
        int start = Math.max(0, pos - radius);
        int end = Math.min(content.length(), pos + term.length() + radius);
        String context = content.substring(start, end);
        if (context.contains("中职")) return true;
        String[] patterns = {
            "不同于" + term, "与" + term + "不同", "和" + term + "不同",
            "相比" + term, "相对于" + term, "区别于" + term,
            "不像" + term, "而非" + term, "不是" + term,
            term + "不同", "不同于普通", term + "相比"
        };
        for (String p : patterns) { if (context.contains(p)) return true; }
        return false;
    }

    /** STUDY_BUDDY 答案泄露检测 */
    public static String checkAnswerLeak(String content) {
        if (content == null || content.isBlank()) return null;
        boolean hasAnswerMarker = content.contains("答案") || content.contains("正确")
                || content.contains("选") && (content.contains("A") || content.contains("B"));
        if (!hasAnswerMarker) return null;
        boolean hasGuidance = content.contains("思考") || content.contains("分析")
                || content.contains("步骤") || content.contains("思路")
                || content.contains("为什么") || content.contains("提示")
                || content.contains("注意") || content.contains("先");
        if (!hasGuidance) {
            return "检测到可能直接给出答案，已标记提醒。请通过引导式提问帮助学生自己得出答案。";
        }
        return null;
    }

    /** 判断文本是否可能含 JSON */
    public static boolean looksLikeJson(String text) {
        return text != null && text.length() < 2000 && text.contains("{");
    }

    /** 简单的 JSON 字符串转义 */
    public static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /** 工具名 → 中文进度描述 */
    public static String toolProgressDescription(String toolName) {
        if (toolName == null) return "正在处理…";
        return switch (toolName) {
            case "teaching_my_classes" -> "正在获取班级信息…";
            case "teaching_class_analytics" -> "正在分析班级成绩数据…";
            case "teaching_knowledge_trend" -> "正在查看知识点掌握趋势…";
            case "teaching_similar_questions" -> "正在从题库匹配相关习题…";
            case "teaching_class_students" -> "正在获取学生名单…";
            case "teaching_student_mastery" -> "正在获取学生掌握度…";
            case "teaching_student_growth" -> "正在查看学生成长曲线…";
            case "teaching_knowledge_search" -> "正在搜索知识库…";
            case "teaching_syllabus_lookup" -> "正在查询考纲…";
            case "teaching_create_task" -> "正在创建教学任务…";
            case "teaching_generate_ppt" -> "正在生成PPT课件…";
            case "teaching_search_tasks" -> "正在搜索已有任务…";
            case "teaching_student_wrong_book" -> "正在查询错题本…";
            case "teaching_aggregate_questions" -> "正在聚合组卷…";
            default -> "正在查询相关数据…";
        };
    }
}