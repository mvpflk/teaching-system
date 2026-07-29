package com.school.teaching.common.score;

import com.school.teaching.common.ScoreCalculator;
import com.school.teaching.common.ScoreType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PassFailCalculator implements ScoreCalculator {

    @Override
    public ScoreType getType() { return ScoreType.PASS_FAIL; }

    @Override
    public boolean isPassed(Map<String, Object> submission, Map<String, Object> config) {
        String gl = toString(submission.get("gradeLevel"));
        return "PASS".equalsIgnoreCase(gl) || "通过".equals(gl);
    }

    @Override
    public int toCreditValue(Map<String, Object> submission) {
        return isPassed(submission, null) ? 2 : 0;
    }

    private String toString(Object v) {
        return v != null ? v.toString() : "";
    }
}
