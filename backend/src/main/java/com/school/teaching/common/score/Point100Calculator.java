package com.school.teaching.common.score;

import com.school.teaching.common.ScoreCalculator;
import com.school.teaching.common.ScoreType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class Point100Calculator implements ScoreCalculator {

    @Override
    public ScoreType getType() { return ScoreType.POINT_100; }

    @Override
    public boolean isPassed(Map<String, Object> submission, Map<String, Object> config) {
        Object scoreObj = submission.get("score");
        BigDecimal score = toBigDecimal(scoreObj);

        // 优先使用 passRate（从上级传入的 config）
        Object passRateObj = config != null ? config.get("passRate") : null;
        Object totalScoreObj = submission.get("totalScore");
        if (passRateObj != null && totalScoreObj != null) {
            int passRate = Integer.parseInt(passRateObj.toString());
            BigDecimal total = toBigDecimal(totalScoreObj);
            if (passRate > 0 && total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = total.multiply(BigDecimal.valueOf(passRate))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                return score.compareTo(threshold) >= 0;
            }
        }

        // 回退: 绝对分值 passingScore（兼容旧数据）
        Object passingObj = config != null ? config.get("passingScore") : null;
        BigDecimal passing = passingObj != null ? new BigDecimal(passingObj.toString()) : BigDecimal.valueOf(60);
        return score.compareTo(passing) >= 0;
    }

    @Override
    public int toCreditValue(Map<String, Object> submission) {
        // 注意：此方法仅供首次提交（isOfficial=true）调用
        // 重测提交不计入门积分，由上游调用方保证
        BigDecimal score = toBigDecimal(submission.get("score"));
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) return 10;
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return 7;
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) return 5;
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return 2;
        return 0;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (v instanceof String s && !s.isEmpty()) {
            try { return new BigDecimal(s); } catch (Exception e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }
}
