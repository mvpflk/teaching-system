package com.school.teaching.utils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * 统计算分工具 — 消除全模块重复模式
 */
public final class ScoreUtils {
    private ScoreUtils() {}

    /** 从预设分值查找题目分值，未找到回退默认值 */
    public static BigDecimal scoreFromPresets(String questionType, Map<String, Integer> presets) {
        if (presets != null) {
            String shortKey = switch (questionType) {
                case "SINGLE_CHOICE" -> "single";
                case "MULTI_CHOICE" -> "multi";
                case "TRUE_FALSE" -> "judge";
                case "FILL_IN" -> "fill";
                default -> "other";
            };
            Integer val = presets.get(questionType);
            if (val == null) val = presets.get(shortKey);
            if (val != null) return BigDecimal.valueOf(val);
        }
        return defaultScore(questionType);
    }

    /** 题目类型默认分值 */
    public static BigDecimal defaultScore(String questionType) {
        if ("SINGLE_CHOICE".equals(questionType)) return BigDecimal.valueOf(2);
        if ("MULTI_CHOICE".equals(questionType)) return BigDecimal.valueOf(3);
        if ("TRUE_FALSE".equals(questionType) || "FILL_IN".equals(questionType)) return BigDecimal.ONE;
        return BigDecimal.TEN;
    }

    /** BigDecimal 集合的平均值 */
    public static double avg(Collection<BigDecimal> values) {
        if (values == null || values.isEmpty()) return 0;
        return values.stream().filter(v -> v != null).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
    }

    /** Double 集合的平均值 */
    public static double avgDouble(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        return values.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** DoubleStream 的平均值 */
    public static double avg(DoubleStream stream) {
        return stream.average().orElse(0);
    }

    /** 标准差 */
    public static double stdDev(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        double mean = avgDouble(values);
        double variance = values.stream().filter(v -> v != null)
            .mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }
}
