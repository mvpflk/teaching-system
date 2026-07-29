package com.school.teaching.common;

/**
 * 评分体系类型 — 前端根据此值动态渲染评分组件。
 */
public enum ScoreType {
    POINT_100,      // 百分制（默认）
    GRADE_5,        // 五级制：A/A-/B+/B/C
    PASS_FAIL,      // 通过/不通过
    CUSTOM_RUBRIC   // 自定义评分标准（rubric 存储在 score_json）
}
