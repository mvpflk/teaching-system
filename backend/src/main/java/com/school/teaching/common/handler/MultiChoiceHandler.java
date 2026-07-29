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
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MultiChoiceHandler implements QuestionTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(MultiChoiceHandler.class);

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.MULTI_CHOICE; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "checkbox");
        r.put("text", q.getQuestionText());
        r.put("options", parseOpts(q.getOptions()));
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请至少选择一个选项");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        return sortTokens(q.getCorrectAnswer()).equals(sortTokens(answer.toString()))
            ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    static String sortTokens(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty())
            .map(String::toUpperCase).sorted().collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    static java.util.List<String> parseOpts(String json) {
        if (json == null || json.isBlank()) return java.util.List.of();
        try {
            if (json.startsWith("[")) return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, java.util.List.class);
        } catch (Exception ignored) { log.error("解析选项JSON失败", ignored); }
        return java.util.List.of(json.split("\\n"));
    }
}
