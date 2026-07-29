package com.school.teaching.service;

import com.school.teaching.entity.TeacherActivity;
import java.util.List;

public interface TeacherActivityService {
    void log(Long teacherId, String action, String targetType, Long targetId);
    List<TeacherActivity> listByTeacher(Long teacherId, int limit);

    /** 渗透自报: 今日是否已勾选 */
    boolean hasCheckedToday(Long teacherId);

    /** 渗透自报: 查询今日勾选状态 (null=未报, true=参考了AI, false=未参考) */
    Boolean getTodayCheck(Long teacherId);

    /** 渗透自报: 查询历史记录 */
    List<TeacherActivity> listPenetrationChecks(Long teacherId, int limit);
}
