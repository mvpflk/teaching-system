package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class MatchingHandler implements QuestionTypeHandler {

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.MATCHING; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "matching");
        r.put("text", q.getQuestionText());
        r.put("pairs", parsePairs(q.getOptions()));
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请完成连线匹配");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        // 正确答案格式: "A-1,B-2,C-3"
        Set<String> correctPairs = new HashSet<>(Arrays.asList(q.getCorrectAnswer().split(",")));
        Set<String> studentPairs = new HashSet<>(Arrays.asList(answer.toString().split(",")));
        if (correctPairs.isEmpty()) return BigDecimal.ZERO;
        long matched = correctPairs.stream().filter(p -> studentPairs.contains(p.trim())).count();
        return BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(correctPairs.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parsePairs(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) { return List.of(); }
    }
}
