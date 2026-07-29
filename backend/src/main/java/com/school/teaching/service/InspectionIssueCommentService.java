package com.school.teaching.service;

import com.school.teaching.entity.InspectionIssueComment;

import java.util.List;

public interface InspectionIssueCommentService {
    InspectionIssueComment addComment(Long issueId, Long userId, String content, Integer isSystem);
    List<InspectionIssueComment> getCommentsByIssue(Long issueId);
}
