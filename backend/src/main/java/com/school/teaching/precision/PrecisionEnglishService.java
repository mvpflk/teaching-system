package com.school.teaching.precision;

import java.util.List;
import java.util.Map;

public interface PrecisionEnglishService {

    Map<String, Object> getDashboard(Long studentId);
    Map<String, Object> buildDailyTask(Long studentId, int stage, Map<String, Object> profile);
    Map<String, Object> submitDrillAnswer(Long studentId, Long questionId, String answer, int hintLevel, String questionType, String subject, String word, String direction);
    Map<String, Object> completeDrill(Long studentId, List<Map<String, Object>> answers, int groupSeq, int elapsedSeconds);
    Map<String, Object> determineStage(Long studentId, List<Map<String, Object>> diagnosisAnswers);
    Map<String, Object> useFreezeCard(Long studentId);
    Map<String, Object> freezeCardStatus(Long studentId);

    Map<String, Object> teacherEnglishStudents(Long classId);
    Map<String, Object> teacherEnglishReport(Long classId);
    int remindClass(Long classId, String message);

    // ── 语法树 + 练习 ──
    List<Map<String, Object>> getGrammarTree(Long studentId);
    List<Map<String, Object>> getGrammarPractice(Long studentId, Long nodeId);
    // ── 阅读 ──
    Map<String, Object> getReading(Long studentId);
    // ── 阶段快测 ──
    List<Map<String, Object>> getStageTest(Long studentId, int stage, String testType);
    // ── 排行 + 词汇本 ──
    List<Map<String, Object>> getClassRanking(Long studentId);
    Map<String, Object> getVocabBook(Long studentId);

    // ── 兼容旧接口 ──
    Map<String, Object> buildEnglishPackData(Long studentId, int weekNo);
    List<Map<String, Object>> buildOnlineTestQuestions(Long studentId);
    Map<String, Object> diagnose(Long studentId);
    Map<String, Object> getEnglishReport(Long studentId);
    Map<String, Object> getWeeklyReadings(Long studentId, int weekNo);
    Map<String, Object> getWeeklyGrammar(Long studentId, int weekNo);
    String getVocabExpected(Long questionId, String prompt);
    String[] getExpectedAndExplanation(Long questionId);
}
