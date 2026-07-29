package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface ParentService {
    /** 获取家长关联的所有孩子基本信息（姓名、班级、学号） */
    List<Map<String, Object>> getMyChildren(Long parentUserId);

    /** 获取指定孩子的成绩列表（任务名称、得分、总分、提交时间），仅限该家长的孩子 */
    List<Map<String, Object>> getChildGrades(Long parentUserId, Long studentId);

    /** 获取指定孩子的成长足迹简要记录，仅限该家长的孩子 */
    List<Map<String, Object>> getChildTimeline(Long parentUserId, Long studentId);

    /** 检查该家长是否有权访问该学生 */
    boolean isMyChild(Long parentUserId, Long studentId);

    /** 家长通过学号+姓名自助绑定孩子 */
    Map<String, Object> bindChild(Long parentUserId, String studentNumber, String studentName, String relation);

    /** 获取指定孩子的作业列表（最近30天），仅限该家长的孩子 */
    List<Map<String, Object>> getChildHomework(Long parentUserId, Long studentId);

    /** 家长确认已读某条预警 */
    void acknowledgeAlert(Long parentUserId, Long alertId);

    /** 获取指定孩子的实训记录 */
    List<Map<String, Object>> getChildPractices(Long parentUserId, Long studentId);
}
