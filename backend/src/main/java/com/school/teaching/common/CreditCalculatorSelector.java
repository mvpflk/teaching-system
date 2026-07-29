package com.school.teaching.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CreditCalculator 策略选择器 — 根据 ruleCode 自动选择对应的积分计算器。
 */
@Component
public class CreditCalculatorSelector {

    private final Map<String, CreditCalculator> calculators;

    @Autowired
    public CreditCalculatorSelector(List<CreditCalculator> calculatorList) {
        this.calculators = calculatorList.stream()
                .collect(Collectors.toMap(CreditCalculator::getRuleCode, c -> c, (a, b) -> b));
    }

    public CreditCalculator getCalculator(String ruleCode) {
        return calculators.getOrDefault(ruleCode, calculators.get("DEFAULT"));
    }
}
