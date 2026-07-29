package com.school.teaching.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ScoreCalculator 策略选择器 — 根据 ScoreType 自动选择对应的评分计算器。
 */
@Component
public class ScoreCalculatorSelector {

    private final Map<ScoreType, ScoreCalculator> calculators;

    @Autowired
    public ScoreCalculatorSelector(List<ScoreCalculator> calculatorList) {
        this.calculators = calculatorList.stream()
                .collect(Collectors.toMap(ScoreCalculator::getType, c -> c, (a, b) -> b));
    }

    public ScoreCalculator getCalculator(ScoreType type) {
        return calculators.get(type);
    }

    public boolean hasCalculator(ScoreType type) {
        return calculators.containsKey(type);
    }
}
