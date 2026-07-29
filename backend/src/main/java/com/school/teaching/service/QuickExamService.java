package com.school.teaching.service;

import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.TeacherClass;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.TeacherClassMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuickExamService {

    private final AiQuestionGeneratorService aiQuestionService;
    private final AiTaskStore taskStore;
    private final TeacherClassMapper teacherClassMapper;
    private final KnowledgeNodeMapper nodeMapper;

    /** 学科→题型模板映射 */
    private static final Map<String, Map<String, Integer>> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("语文", Map.of("SINGLE_CHOICE", 2, "TRUE_FALSE", 2, "FILL_IN", 1, "READING_COMPREHENSION", 1));
        TEMPLATES.put("数学", Map.of("SINGLE_CHOICE", 2, "TRUE_FALSE", 1, "FILL_IN", 1, "CALCULATION", 2));
        TEMPLATES.put("英语", Map.of("SINGLE_CHOICE", 2, "CLOZE", 1, "READING_COMPREHENSION", 1, "FILL_IN", 2));
    }

    /** 专业课默认模板 */
    private static final Map<String, Integer> DEFAULT_TEMPLATE = Map.of("SINGLE_CHOICE", 3, "TRUE_FALSE", 2, "FILL_IN", 1);

    /** 获取教师的任教学科列表（从 teacher_classes.subject 逗号分隔字段提取） */
    public List<String> getTeacherSubjects(Long teacherId) {
        List<TeacherClass> tcs = teacherClassMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, teacherId));
        Set<String> subjects = new LinkedHashSet<>();
        for (TeacherClass tc : tcs) {
            if (tc.getSubject() != null) {
                for (String part : tc.getSubject().split("[,，、]")) {
                    String s = part.trim();
                    if (!s.isEmpty()) subjects.add(s);
                }
            }
        }
        return new ArrayList<>(subjects);
    }

    /** 根据学科名获取题型模板 */
    public Map<String, Integer> getTemplate(String subject) {
        if (subject == null) return DEFAULT_TEMPLATE;
        for (var entry : TEMPLATES.entrySet()) {
            if (subject.contains(entry.getKey())) return entry.getValue();
        }
        return DEFAULT_TEMPLATE;
    }

    /** 异步执行一键生成 */
    @Async("aiExecutor")
    public void executeAsync(String taskId, Long teacherId, String subject, Long categoryId) {
        taskStore.markRunning(taskId);
        try {
            Map<String, Integer> typeCounts = getTemplate(subject);
            Map<String, Object> extraParams = new LinkedHashMap<>();
            extraParams.put("subject", subject);
            extraParams.put("typeCounts", typeCounts);
            extraParams.put("difficulty", 2);
            extraParams.put("count", typeCounts.values().stream().mapToInt(Integer::intValue).sum());

            // 注入知识树信息
            if (categoryId != null) {
                KnowledgeNode node = nodeMapper.selectById(categoryId);
                if (node != null) {
                    extraParams.put("categoryId", categoryId);
                    extraParams.put("knowledgePoint", node.getName());
                    // 递归收集子知识点
                    List<Map<String, Object>> subKps = new ArrayList<>();
                    collectAllSubKps(categoryId, subKps, new HashSet<>());
                    if (!subKps.isEmpty()) {
                        extraParams.put("_subKpList", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(subKps));
                    }
                }
            }

            List<Map<String, Object>> questions = aiQuestionService.generateFromAiAssistant(teacherId, extraParams);

            // 题目质量校验（R111）
            int removed = 0;
            if (questions != null) {
                var iter = questions.iterator();
                while (iter.hasNext()) {
                    var q = iter.next();
                    String qType = String.valueOf(q.getOrDefault("questionType", ""));
                    @SuppressWarnings("unchecked")
                    List<String> opts = (List<String>) q.get("options");
                    // 选择题：检查答案是否在选项范围内
                    if (("SINGLE_CHOICE".equals(qType) || "MULTI_CHOICE".equals(qType)) && opts != null) {
                        Object ansObj = q.get("correctAnswer");
                        String ans = ansObj != null ? String.valueOf(ansObj) : "";
                        String letters = ans.replaceAll("[^A-Za-z]", "").toUpperCase();
                        char maxLetter = (char) ('A' + Math.min(opts.size(), 26) - 1);
                        for (char c : letters.toCharArray()) {
                            if (c > maxLetter) {
                                log.warn("QuickExam: 跳过无效题 — 答案{}超出选项范围(A-{}) qType={}", c, maxLetter, qType);
                                iter.remove();
                                removed++;
                                break;
                            }
                        }
                    }
                }
                if (removed > 0) log.warn("QuickExam: 过滤了 {} 道无效题，剩余 {} 道", removed, questions.size());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "questions");
            result.put("questions", questions != null ? questions : List.of());
            result.put("count", questions != null ? questions.size() : 0);
            result.put("subject", subject);
            taskStore.complete(taskId, result);
        } catch (Exception e) {
            log.error("QuickExam生成失败: taskId={}", taskId, e);
            taskStore.fail(taskId, e.getMessage());
        }
    }

    /** 递归收集节点下所有子孙知识点（L3/L4），用于一键出题的知识树约束 */
    private void collectAllSubKps(Long nodeId, List<Map<String, Object>> result, Set<Long> visited) {
        if (nodeId == null || !visited.add(nodeId)) return;
        List<KnowledgeNode> children = nodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getParentId, nodeId));
        if (children == null || children.isEmpty()) return;
        for (KnowledgeNode child : children) {
            if (child.getLevel() != null && child.getLevel() >= 3 && child.getContent() != null && !child.getContent().isBlank()) {
                result.add(Map.of("id", child.getId(), "name", child.getName() != null ? child.getName() : ""));
            }
            if (child.getLevel() != null && child.getLevel() <= 3) {
                collectAllSubKps(child.getId(), result, visited);
            }
        }
    }
}
