package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface SystemService {
    // Settings
    Map<String, String> getAllSettings();
    boolean getBooleanConfig(String key, boolean defaultValue);
    int getIntConfig(String key, int defaultValue);
    Map<String, Boolean> getFeatureFlags();
    void updateAllSettings(Map<String, String> settings);
    List<Map<String, Object>> getGrades(Long stageId);
    List<Map<String, Object>> getSubjects();
    List<Map<String, Object>> getDictGrades();
    List<Map<String, Object>> getDictSubjects();
    Map<String, Object> addDictGrade(String name);
    void updateDictGrade(Long id, String name);
    void deleteDictGrade(Long id);
    int batchAddDictGrades(List<Map<String, Object>> list);
    Map<String, Object> addDictSubject(String name);
    void updateDictSubject(Long id, String name);
    void deleteDictSubject(Long id);
    int batchAddDictSubjects(List<Map<String, Object>> list);
    List<Map<String, Object>> getDictMajors();
    Map<String, Object> addDictMajor(String name);
    void updateDictMajor(Long id, String name);
    void deleteDictMajor(Long id);
    int batchAddDictMajors(List<Map<String, Object>> list);
    /** 专业-学科关联 */
    List<Map<String, Object>> getMajorSubjects(Long majorId);
    void setMajorSubjects(Long majorId, List<Long> subjectIds);
    Map<String, Object> resetData(String target);
    Map<String, Object> getSystemInfo();

    // Dynamic params
    List<Map<String, Object>> getDynamicParams(String category);
    Map<String, Object> getParamDetail(String key);
    void updateParam(String key, String value);
    void updateParamsBatch(List<Map<String, String>> params);

    // Announcement
    int sendAnnouncement(String scope, Long targetId, String title, String content);

    // Dashboard
    Map<String, Object> getDashboardOverview();
    List<Map<String, Object>> getSystemParams(String category);
    List<Map<String, Object>> getSystemParams();
    void updateSystemParams(Map<String, Object> params);

    // Maintenance
    byte[] exportBackup();
    int importBackup(byte[] sqlBytes);
    Map<String, Object> clearAllData();

    /** 学期管理 */
    List<Map<String, Object>> getTerms();
    Map<String, Object> addTerm(Map<String, Object> body);
    void updateTerm(Long id, Map<String, Object> body);
    void deleteTerm(Long id);

    /** 偏科提分班级权限 */
    String getRemedialClassIds();
    void setRemedialClassIds(String classIds);
    List<Map<String, Object>> getAllClassesWithRemedialStatus();
    boolean isRemedialEnabledForCurrentUser();

    /** 系统 Logo */
    String getLogoUrl();
    void setLogoUrl(String url);

    /** 获取职高学科列表（subject_name 含 [职高]） */
    List<String> getVocationalSubjects();
}
