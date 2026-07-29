package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public final class AiContentHelper {

    private AiContentHelper() {}

    private static final ObjectMapper om = new ObjectMapper();

    public static String cleanMarkdownForTask(String content) {
        int firstHash = content.indexOf('#');
        if (firstHash > 0) content = content.substring(firstHash);
        content = content.replaceAll("```json\\s*\\{[^`]*\\}\\s*```", "");
        content = content.replaceAll("## 六、任务元数据JSON.*", "");
        content = content.replaceAll("\n{3,}", "\n\n");
        content = content.replaceAll("(?m)^###\\s+任务(\\d+)", "【任务$1】");
        content = content.replaceAll("(?m)^###\\s+", "■ ");
        content = content.replaceAll("(?m)^##\\s+", "◆ ");
        content = content.replaceAll("(?m)^#\\s+", "");
        content = content.replaceAll("\\*\\*(.+?)\\*\\*", "【$1】");
        return content.trim();
    }

    public static String filterRelevantSections(String content, String targetName, int maxSections) {
        if (content == null || content.isEmpty()) return "";
        String[] sections = content.split("(?m)(?=^#{1,3}\\s)");
        if (sections.length <= 1 && sections.length <= maxSections) return content;
        if (sections.length <= maxSections) return content;

        String[] keywords = (targetName != null ? targetName : "").split("[\\s，,、/|]+");
        int[] scores = new int[sections.length];
        for (int i = 0; i < sections.length; i++) {
            String lower = sections[i].toLowerCase();
            for (String kw : keywords) {
                if (kw.length() >= 2 && lower.contains(kw.toLowerCase())) scores[i] += kw.length() * kw.length();
            }
        }

        Integer[] indices = new Integer[sections.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> Integer.compare(scores[b], scores[a]));

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(maxSections, sections.length); i++) {
            if (scores[indices[i]] > 0 || i == 0) {
                result.append(sections[indices[i]].trim()).append("\n\n");
            }
        }
        return result.toString();
    }

    public static int relevanceScore(String target, String candidate) {
        if (target == null || candidate == null || target.isEmpty() || candidate.isEmpty()) return 0;
        int score = 0;
        for (int i = 0; i < target.length() - 1; i++) {
            if (candidate.contains(target.substring(i, Math.min(i + 2, target.length())))) score++;
        }
        return score;
    }

    public static Task copyTask(Task src, Long targetId) {
        Task copy = new Task();
        copy.setTitle(src.getTitle());
        copy.setDescription(src.getDescription());
        copy.setTaskType(src.getTaskType());
        copy.setScoreType(src.getScoreType());
        copy.setTotalScore(src.getTotalScore());
        copy.setGradeId(src.getGradeId());
        copy.setDeadline(src.getDeadline());
        copy.setAllowCustomSteps(src.getAllowCustomSteps());
        copy.setSubject(src.getSubject());
        copy.setTeacherId(src.getTeacherId());
        copy.setTargetType(src.getTargetType());
        copy.setTargetId(targetId);
        copy.setSchoolId(src.getSchoolId());
        return copy;
    }

    public static String extractBatchId(QuestionBank q) {
        try {
            if (q.getContentJson() != null) {
                var node = om.readTree(q.getContentJson());
                if (node.has("batchId")) return node.get("batchId").asText();
            }
        } catch (Exception ignored) { log.warn("batchId解析失败", ignored); }
        return "batch_" + q.getCreatedBy() + "_" + q.getCreateTime().toLocalDate().toString();
    }

    @SuppressWarnings("unchecked")
    public static Object parseOptionsSafe(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return Collections.emptyList();
        try { return om.readValue(optionsJson, java.util.List.class); } catch (Exception e) { return Collections.emptyList(); }
    }
}
