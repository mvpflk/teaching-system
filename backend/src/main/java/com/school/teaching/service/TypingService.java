package com.school.teaching.service;

import com.school.teaching.entity.*;

import java.util.List;
import java.util.Map;

public interface TypingService {
    // ── 权限 ──
    boolean checkStudentPermission(Long studentId);
    List<Integer> getTypingAllowedMajors();
    void setTypingAllowedMajors(List<Integer> majorIds);

    // ── 文本管理 ──
    Map<String, Object> getTexts(int page, int size, String type, String keyword,
                                 String language, Integer difficulty, String category);
    TypingText addText(TypingText text);
    TypingText updateText(Long id, TypingText text);
    void deleteText(Long id);

    // ── 竞赛管理 ──
    Map<String, Object> getCompetitions(int page, int size, String status);
    TypingCompetition createCompetition(TypingCompetition comp);
    void startCompetition(Long id);
    void finishCompetition(Long id);
    void deleteCompetition(Long id);
    TypingCompetition getCurrentCompetition(Long studentId);

    // ── 排名/驾驶舱 ──
    List<Map<String, Object>> getRanking(Long competitionId);
    Map<String, Object> getDashboard(Long competitionId);

    // ── 学生打字过程 ──
    void reportProgress(Long competitionId, Long studentId, Map<String, Object> progress);
    TypingText getRandomPracticeText(Long textId, Integer difficulty, String language);
    void saveRecord(TypingRecord record);
    /** #15 修复：统一练习保存入口（校验 + 保存 + 加经验），返回获得的经验值 */
    int savePracticeRecord(TypingRecord record);
    List<String> getPracticeCategories();

    // ── 提交成绩 ──
    TypingCompetitionResult submitResult(Long competitionId, Long studentId, Map<String, Object> data);

    // ── 速度趋势 ──
    /** 获取用户历史速度趋势（最近 N 条记录） */
    List<Map<String, Object>> getStudentSpeedTrend(Long studentId, int limit);

    // ── 竞赛回放 ──
    Map<String, Object> getCompetitionReplay(Long competitionId, Long studentId);

    // ── 导出 ──
    List<Map<String, Object>> exportResults(Long competitionId);

    // ── 学生历史/游戏化 ──
    List<TypingRecord> getStudentHistory(Long studentId);
    List<Map<String, Object>> getWrongWords(Long studentId);
    Map<String, Object> getStudentLevels(Long studentId);
    void addExp(Long studentId, int exp);
}
