package com.school.teaching.agent.loop;

import java.util.List;
import java.util.Map;

public enum AgentContentType {

    KNOWLEDGE_CARD("knowledge_card", "知识卡片", "概念解释、知识点讲解、术语定义",
            List.of("title", "subject", "summary", "key_points", "examples"),
            List.of("title", "subject")),

    VOCABULARY_LIST("vocabulary_list", "词汇/术语表", "英语单词、IT术语、农学术语",
            List.of("title", "subject", "source", "items"),
            List.of("title", "items")),

    EXERCISE_SET("exercise_set", "练习题组", "随堂练习、课后作业、自我检测",
            List.of("title", "subject", "difficulty", "questions"),
            List.of("title", "questions")),

    STEP_BY_STEP("step_by_step", "分步求解", "数学计算、编程调试、实训操作步骤",
            List.of("title", "subject", "steps"),
            List.of("title", "steps")),

    ANALYSIS_REPORT("analysis_report", "分析报告", "成绩分析、掌握度诊断、趋势分析",
            List.of("title", "subject", "summary", "metrics"),
            List.of("title", "summary")),

    COMPARISON("comparison", "对比表", "概念对比、方案对比、数据对比",
            List.of("title", "headers", "rows"),
            List.of("title", "headers", "rows")),

    LEARNING_PATH("learning_path", "学习路径", "个性化学习推荐、知识树进度",
            List.of("title", "subject", "nodes"),
            List.of("title", "nodes"));

    private final String type;
    private final String label;
    private final String description;
    private final List<String> fields;
    private final List<String> required;

    AgentContentType(String type, String label, String description,
                     List<String> fields, List<String> required) {
        this.type = type;
        this.label = label;
        this.description = description;
        this.fields = fields;
        this.required = required;
    }

    public String getType() { return type; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public List<String> getFields() { return fields; }
    public List<String> getRequired() { return required; }

    public static AgentContentType fromType(String type) {
        for (AgentContentType ct : values()) {
            if (ct.type.equals(type)) return ct;
        }
        return null;
    }

    public static String buildSchemaPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据用户问题选择以下一种 JSON 格式输出，type 字段必须匹配：\n\n");
        for (AgentContentType ct : values()) {
            sb.append("- ").append(ct.type).append("：").append(ct.description).append("\n");
            sb.append("  必填：").append(String.join("、", ct.required)).append("\n");
        }
        sb.append("\n输出必须是合法 JSON，type 字段放在最顶层。");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static boolean validate(Map<String, Object> data) {
        Object typeObj = data.get("type");
        if (!(typeObj instanceof String)) return false;
        AgentContentType ct = fromType((String) typeObj);
        if (ct == null) return false;
        for (String r : ct.required) {
            Object val = data.get(r);
            if (val == null) return false;
            if (val instanceof String s && s.isBlank()) return false;
            if (val instanceof List l && l.isEmpty()) return false;
        }
        return true;
    }
}
