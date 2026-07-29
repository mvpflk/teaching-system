package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface StudentRemarkService {

    /**
     * 获取班级学生列表（含本学期评语）
     */
    List<Map<String, Object>> studentsWithRemarks(Long classId);

    /**
     * 修改/新增学生评语
     */
    void updateRemark(Long studentId, String remark);

    /**
     * 获取学生成长报告
     */
    Map<String, Object> growthReport(Long userId);
}
