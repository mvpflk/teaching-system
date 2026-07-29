package com.school.teaching.common.score;

import com.school.teaching.common.ScoreCalculator;
import com.school.teaching.common.ScoreType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CustomRubricCalculator implements ScoreCalculator {

    @Override
    public ScoreType getType() { return ScoreType.CUSTOM_RUBRIC; }

    @Override
    public boolean isPassed(Map<String, Object> submission, Map<String, Object> config) {
        BigDecimal score = toDecimal(submission.get("score"));
        BigDecimal passing = config != null ? toDecimal(config.get("passingScore")) : BigDecimal.valueOf(60);
        return score.compareTo(passing) >= 0;
    }

    @Override
    public int toCreditValue(Map<String, Object> submission) {
        BigDecimal score = toDecimal(submission.get("score"));
        // 按百分制比例折算积分: 100分→10积分, 按比例缩放
        if (score.compareTo(BigDecimal.ZERO) <= 0) return 0;
        if (score.compareTo(BigDecimal.valueOf(100)) >= 0) return 10;
        // 向上取整，最低 1 分
        int credits = score.multiply(BigDecimal.valueOf(10))
            .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).intValue();
        return Math.max(1, credits);
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        try { return new BigDecimal(v.toString()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
