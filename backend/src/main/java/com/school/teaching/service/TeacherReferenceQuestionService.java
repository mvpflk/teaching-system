package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.TeacherReferenceQuestion;
import com.school.teaching.mapper.TeacherReferenceQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherReferenceQuestionService {

    private final TeacherReferenceQuestionMapper mapper;
    private final ObjectMapper objectMapper;

    public String loadRandom(String subject, String questionType) {
        List<TeacherReferenceQuestion> list = mapper.selectList(
            new LambdaQueryWrapper<TeacherReferenceQuestion>()
                .eq(TeacherReferenceQuestion::getSubject, subject)
                .eq(TeacherReferenceQuestion::getQuestionType, questionType)
                .eq(TeacherReferenceQuestion::getEnabled, 1));
        if (list.isEmpty()) {
            return "";
        }
        TeacherReferenceQuestion picked = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        return formatReference(picked);
    }

    /**
     * 数据驱动：查该学科所有已启用的样题，按题型分组，每种随机取 1 道。
     * 不同学科自动覆盖各自的特色题型——语文的阅读问答、数学的解答题/证明题、英语的完形填空/阅读理解等。
     * 返回拼接后的文本，无数据时返回空串（零配置退化）。
     */
    public String loadAllTypes(String subject) {
        List<TeacherReferenceQuestion> all = mapper.selectList(
            new LambdaQueryWrapper<TeacherReferenceQuestion>()
                .eq(TeacherReferenceQuestion::getSubject, subject)
                .eq(TeacherReferenceQuestion::getEnabled, 1));
        if (all.isEmpty()) {
            return "";
        }
        // 按题型分组（LinkedHashMap 保持题型顺序稳定）
        Map<String, List<TeacherReferenceQuestion>> grouped = new LinkedHashMap<>();
        for (TeacherReferenceQuestion q : all) {
            grouped.computeIfAbsent(q.getQuestionType(), k -> new ArrayList<>()).add(q);
        }
        // 每种题型随机取 1 道
        List<String> parts = new ArrayList<>();
        for (List<TeacherReferenceQuestion> group : grouped.values()) {
            TeacherReferenceQuestion picked = group.get(ThreadLocalRandom.current().nextInt(group.size()));
            String formatted = formatReference(picked);
            if (!formatted.isEmpty()) {
                parts.add(formatted);
            }
        }
        return parts.isEmpty() ? "" : String.join("\n", parts);
    }

    @SuppressWarnings("unchecked")
    private String formatReference(TeacherReferenceQuestion ref) {
        try {
            Map<String, Object> data = objectMapper.readValue(ref.getContentJson(), Map.class);
            StringBuilder sb = new StringBuilder();
            sb.append("（").append(ref.getQuestionType()).append("参考样题）\n");
            String questionText = (String) data.get("questionText");
            if (questionText != null) sb.append(questionText).append("\n");
            Object options = data.get("options");
            if (options instanceof List) {
                for (Object opt : (List<Object>) options) {
                    sb.append(opt).append("\n");
                }
            }
            String answer = (String) data.get("correctAnswer");
            if (answer != null) sb.append("答案：").append(answer).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("格式化参考样题失败: id={}", ref.getId(), e);
            return "";
        }
    }
}
