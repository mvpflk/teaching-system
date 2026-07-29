package com.school.teaching.common;

/**
 * 异步任务类型枚举 — 每类任务有独立默认超时
 */
public enum AsyncTaskType {
    AI_GENERATE("AI内容/出题生成", 180),
    AI_GRADING("AI评分", 30),
    ZIP_EXPORT("实训ZIP打包导出", 300),
    AI_SUPPLEMENT("错题AI补充题", 180);

    private final String label;
    private final int defaultTimeoutSeconds;

    AsyncTaskType(String label, int defaultTimeoutSeconds) {
        this.label = label;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public String getLabel() { return label; }
    public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
}
