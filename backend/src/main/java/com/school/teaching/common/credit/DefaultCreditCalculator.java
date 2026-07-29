package com.school.teaching.common.credit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.CreditCalculator;
import com.school.teaching.entity.CreditRule;
import com.school.teaching.mapper.CreditRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 默认积分计算器 — 从 credit_rules 表读取规则并评估条件。
 * 与 ScoreCalculator 配合使用：ScoreCalculator 提供基础分→积分映射，此类提供额外奖励积分。
 */
@Component
@RequiredArgsConstructor
public class DefaultCreditCalculator implements CreditCalculator {

    private final CreditRuleMapper creditRuleMapper;

    @Override
    public String getRuleCode() { return "DEFAULT"; }

    @Override
    public int calculate(Map<String, Object> context) {
        String actionType = toString(context.get("actionType"));
        if (actionType.isEmpty()) return 0;

        List<CreditRule> rules = creditRuleMapper.selectList(
            new LambdaQueryWrapper<CreditRule>()
                .eq(CreditRule::getActionType, actionType)
                .eq(CreditRule::getStatus, 1));

        int total = 0;
        for (CreditRule rule : rules) {
            if (evaluateConditions(rule.getConditions(), context)) {
                total += rule.getCreditValue();
            }
        }
        return total;
    }

    /** 简单条件评估：conditions JSON 中的 key=value 与 context 匹配。支持 minScore/maxScore 范围比较。 */
    @SuppressWarnings("unchecked")
    private boolean evaluateConditions(String conditionsJson, Map<String, Object> context) {
        if (conditionsJson == null || conditionsJson.isBlank()) return true;
        try {
            Map<String, Object> cond = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(conditionsJson, Map.class);
            for (Map.Entry<String, Object> e : cond.entrySet()) {
                Object actual = context.get(e.getKey());
                if (e.getKey().startsWith("min")) {
                    if (!ge(actual, e.getValue())) return false;
                } else if (e.getKey().startsWith("max")) {
                    if (!le(actual, e.getValue())) return false;
                } else {
                    if (!e.getValue().equals(actual)) return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return true; // 条件解析失败时宽松处理，不阻断积分发放
        }
    }

    private boolean ge(Object actual, Object expected) {
        if (actual == null) return false;
        return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected.toString())) >= 0;
    }

    private boolean le(Object actual, Object expected) {
        if (actual == null) return false;
        return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected.toString())) <= 0;
    }

    private String toString(Object v) { return v != null ? v.toString() : ""; }
}
