package com.school.teaching.common;

import java.util.Map;

/**
 * 积分计算器 — 策略接口，可插拔扩展新积分规则。
 *
 * 不同任务类型/评分体系可能对应不同的积分奖励规则。
 * 实现此接口并注册到 Spring Context 即可扩展。
 */
public interface CreditCalculator {

    /**
     * 计算此次提交应获得的积分。
     *
     * @param context 上下文数据：
     *   - taskCategory: 任务分类 (PRE_CLASS/IN_CLASS/...)
     *   - scoreType: 评分类型 (POINT_100/GRADE_5/...)
     *   - isLate: 是否迟交
     *   - isPassed: 是否通过
     *   - currentStreak: 连续提交次数
     *   - score: 得分
     */
    int calculate(Map<String, Object> context);

    /** 返回此计算器适用的规则码（对应 credit_rules.rule_code） */
    default String getRuleCode() { return "DEFAULT"; }
}
