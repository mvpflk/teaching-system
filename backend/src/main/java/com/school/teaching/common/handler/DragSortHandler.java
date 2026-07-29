package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DragSortHandler implements QuestionTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(DragSortHandler.class);

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.DRAG_SORT; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "dragsort");
        r.put("text", q.getQuestionText());
        r.put("items", parseOpts(q.getOptions()));
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请完成排序");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        // 答案格式: "A,C,B" → 学生排序与正确答案完全一致才得分
        String correct = q.getCorrectAnswer().trim();
        String student = answer.toString().trim();
        return correct.equalsIgnoreCase(student) ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    static List<String> parseOpts(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            if (json.startsWith("[")) return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception ignored) { log.error("解析拖拽排序选项JSON失败", ignored); }
        return Arrays.asList(json.split("\\n"));
    }
}
