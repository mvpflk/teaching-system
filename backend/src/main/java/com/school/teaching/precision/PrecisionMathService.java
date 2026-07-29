package com.school.teaching.precision;

import java.util.List;
import java.util.Map;

public interface PrecisionMathService {

    /** 数学诊断：11模块各3题，自适应跳题 */
    java.util.Map<String, Object> diagnose(Long studentId);

    /** 生成数学学习包内容 */
    java.util.Map<String, Object> buildMathPackData(Long studentId, int weekNo);

    /** 生成数学线上小测试题（4原题变数字+3变式+3遗忘检测） */
    java.util.List<java.util.Map<String, Object>> buildOnlineTestQuestions(Long studentId);

    /** 生成数学线上小测试题：传入本周学习包题目ID，实现真正的原题变数字 */
    java.util.List<java.util.Map<String, Object>> buildOnlineTestQuestionsFromPack(Long studentId, java.util.List<Long> packQuestionIds);

    /** AI答疑：数学问题 → AI解析 */
    String aiExplain(Long studentId, String question);
}
