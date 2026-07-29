package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.InspectionIssue;

import java.util.Map;

public interface InspectionIssueService {
    InspectionIssue getById(Long id);
    InspectionIssue create(InspectionIssue issue);
    InspectionIssue update(Long id, InspectionIssue issue);
    void delete(Long id);
    IPage<InspectionIssue> getPage(String status, String category, String severity,
                                   Long assignedTo, Long assignedClassId, Long createdBy,
                                   String startDate, String endDate,
                                   int page, int size);
    InspectionIssue startProgress(Long issueId, Long teacherId);
    InspectionIssue assignIssue(Long issueId, Long teacherId, String deadline);
    InspectionIssue resolveIssue(Long issueId, Long teacherId, String resolveComment);
    InspectionIssue verifyIssue(Long issueId, Long inspectorId, boolean approved, String verifyComment);
    Map<String, Object> getIssueStats();
    IPage<InspectionIssue> getMyAssignedIssues(Long teacherId, String status, int page, int size);
    InspectionIssueCommentService getCommentService();
}
