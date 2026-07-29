package com.school.teaching.service;

import com.school.teaching.entity.AlertRecord;
import com.school.teaching.entity.AlertRule;

import java.util.List;
import java.util.Map;

public interface AlertService {
    /** 获取所有启用的规则 */
    List<AlertRule> getEnabledRules();

    /** 获取规则列表（内置 + 该教师自定义） */
    List<AlertRule> getRules(Long teacherUserId);

    /** 创建或更新规则（仅自定义规则可增删） */
    AlertRule saveRule(AlertRule rule, Long operatorUserId);

    /** 删除自定义规则 */
    void deleteRule(Long ruleId, Long operatorUserId);

    /** 教师查询预警记录列表（按班级权限过滤） */
    Map<String, Object> getAlertRecords(Long teacherUserId, Long classId, String alertType,
                                        String handledStatus, String studentName, int page, int pageSize);

    /** 处理预警（标记已读/已联系/忽略） */
    void handleAlert(Long recordId, String status, Long operatorUserId);

    /** 获取家长关联孩子的预警列表 */
    List<Map<String, Object>> getChildAlerts(Long parentUserId);

    /** 获取家长关联孩子的未读预警数 */
    int getChildUnreadAlertCount(Long parentUserId);

    /** 定时扫描所有学生 */
    int scanAllStudents();

    /** 扫描指定教师管辖班级的学生（班主任手动触发） */
    int scanTeacherClasses(Long teacherUserId);

    /** 增量扫描（自上次扫描后有新提交的学生） */
    int scanIncremental();

    /** 近7天预警趋势 */
    List<Map<String, Object>> getAlertTrend(Long teacherUserId);
}
