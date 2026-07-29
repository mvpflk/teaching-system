package com.school.teaching.service;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/**
 * 轻量成长分析服务 — 复用现有表数据
 */
public interface AnalyticsService {
    /** 成长曲线：按周聚合掌握度 → [{week, masteryPercent, change}] */
    List<Map<String, Object>> getGrowthCurve(Long studentId, String subject);
    /** 知识雷达：每个知识点的掌握度 → [{nodeId, masteryPercent, status}] */
    List<Map<String, Object>> getKnowledgeRadar(Long studentId, String subject);
    /** 成就清单：返回全部成就（earned=true/false） */
    Map<String, Object> getAchievements(Long studentId);
    /** 每日鼓励：根据薄弱点匹配鼓励文案 */
    Map<String, Object> getDailyEncouragement(Long studentId);
    /** 班级成长曲线聚合：全班学生曲线 + 班级平均 */
    List<Map<String, Object>> getClassGrowthCurves(Long classId, String subject);
    /** 学生概要统计：总练习/掌握数/连续周/最近诊断分 */
    Map<String, Object> getStudentSummary(Long studentId, String subject);
    /** 获取学生班级开设的学科列表（用于前端学科下拉框） */
    List<String> getStudentAvailableSubjects(Long studentId);
    /** 导出班级成绩CSV */
    String exportScores(Long classId, String subject, LocalDate startDate, LocalDate endDate, boolean blinded);
    /** E5: 导出课题研究数据 */
    byte[] exportResearchData(boolean blinded);
    /** E6: 班级知识点掌握趋势 */
    List<Map<String, Object>> getKnowledgeTrend(Long classId, Long knowledgeNodeId, String subject, LocalDate startDate, LocalDate endDate);
}
