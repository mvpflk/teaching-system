package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SingleChoiceHandler implements QuestionTypeHandler {

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.SINGLE_CHOICE; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "radio");
        r.put("text", q.getQuestionText());
        try { r.put("options", parseJson(q.getOptions())); } catch (Exception e) { r.put("options", List.of()); }
        r.put("correctAnswer", q.getCorrectAnswer());
        r.put("explanation", q.getExplanation());
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请选择一个选项");
        String a = answer.toString().trim().toUpperCase();
        if (a.length() != 1 || a.charAt(0) < 'A' || a.charAt(0) > 'Z')
            throw new BusinessException(400, "答案格式错误");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        String correct = q.getCorrectAnswer();
        if (correct == null || answer == null) return BigDecimal.ZERO;
        return correct.trim().equalsIgnoreCase(answer.toString().trim())
            ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        if (json.startsWith("[")) {
            try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class); }
            catch (Exception e) { return List.of(); }
        }
        return List.of(json.split("\\n"));
    }
}
