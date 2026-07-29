package com.school.teaching.common;

import java.util.Map;

/**
 * 评分计算器 — 策略接口，可插拔扩展新评分规则。
 *
 * 当新增一种 ScoreType（如 GRADE_5、CUSTOM_RUBRIC）时，
 * 实现此接口并注册到 Spring Context 即可，无需修改现有评分逻辑。
 */
public interface ScoreCalculator {

    /** 返回此计算器处理的评分类型 */
    ScoreType getType();

    /**
     * 判断是否通过。
     * @param submission  提交数据（含 totalScore/gradeLevel/scoreJson）
     * @param config      任务配置（task_config JSON，含 passingScore 等）
     */
    boolean isPassed(Map<String, Object> submission, Map<String, Object> config);

    /**
     * 转换为积分值。
     * 不同评分体系有不同的积分映射规则：
     *   POINT_100: >=90→20, >=80→10, >=60→5
     *   GRADE_5:   A→5, B→3, C→1
     *   PASS_FAIL: 通过→5
     */
    int toCreditValue(Map<String, Object> submission);

    /**
     * 验证评分数据合法性。
     * @param gradeInput 教师输入的评分数据
     * @return null=合法, 非null=错误信息
     */
    default String validate(Map<String, Object> gradeInput) { return null; }
}
