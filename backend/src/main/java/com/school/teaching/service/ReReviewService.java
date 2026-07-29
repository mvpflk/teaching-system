package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReReviewService {

    private final com.school.teaching.mapper.ReReviewRequestMapper reReviewMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final NotificationService notificationService;
    private final StudentEventLogService eventLogService;

    /** 学生申请复议 */
    @Transactional
    public Map<String, Object> requestReReview(Long submissionId, Long studentId, String reason) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        if (!sub.getStudentId().equals(studentId)) throw new BusinessException(403, "无权申请");

        // 检查是否已有待处理申请
        Long exists = reReviewMapper.selectCount(new LambdaQueryWrapper<com.school.teaching.entity.ReReviewRequest>()
            .eq(com.school.teaching.entity.ReReviewRequest::getSubmissionId, submissionId)
            .eq(com.school.teaching.entity.ReReviewRequest::getStatus, "PENDING"));
        if (exists > 0) throw new BusinessException(409, "已有待处理的复议申请");

        // 限制每份提交最多2次复议
        Long totalRequests = reReviewMapper.selectCount(new LambdaQueryWrapper<com.school.teaching.entity.ReReviewRequest>()
            .eq(com.school.teaching.entity.ReReviewRequest::getSubmissionId, submissionId));
        if (totalRequests >= 2) throw new BusinessException(429, "该提交复议次数已达上限（最多2次）");

        com.school.teaching.entity.ReReviewRequest req = new com.school.teaching.entity.ReReviewRequest();
        req.setSubmissionId(submissionId); req.setStudentId(studentId);
        req.setReason(reason); req.setStatus("PENDING");
        reReviewMapper.insert(req);

        // 通知教师
        Task task = new Task(); task.setId(sub.getTaskId()); // simplified: get teacher from task
        eventLogService.log(studentId, "REVIEW_REQUESTED", Map.of("submissionId", submissionId), "TASK");
        return Map.of("requestId", req.getId(), "status", "PENDING");
    }

    /** 教师处理复议 — 同意则允许修改分数，拒绝则记录理由 */
    @Transactional
    public Map<String, Object> resolveReReview(Long requestId, boolean approved, String teacherComment, Long teacherId) {
        com.school.teaching.entity.ReReviewRequest req = reReviewMapper.selectById(requestId);
        if (req == null) throw new BusinessException(404, "复议申请不存在");
        if (!"PENDING".equals(req.getStatus())) throw new BusinessException(409, "申请已处理");

        req.setStatus(approved ? "APPROVED" : "REJECTED");
        req.setTeacherComment(teacherComment);
        reReviewMapper.updateById(req);

        // 复议通过：将提交状态重置为SUBMITTED，允许教师重新评分
        if (approved) {
            TaskSubmission sub = submissionMapper.selectById(req.getSubmissionId());
            if (sub != null) {
                sub.setStatus("SUBMITTED");
                sub.setScore(null);
                sub.setGradeLevel(null);
                submissionMapper.updateById(sub);
            }
        }

        eventLogService.log(req.getStudentId(), "REVIEW_RESOLVED",
            Map.of("requestId", requestId, "approved", approved, "comment", teacherComment != null ? teacherComment : ""), "TASK");
        return Map.of("requestId", requestId, "status", req.getStatus());
    }

    /** 获取学生的复议申请列表 */
    public List<com.school.teaching.entity.ReReviewRequest> getStudentRequests(Long studentId) {
        return reReviewMapper.selectList(new LambdaQueryWrapper<com.school.teaching.entity.ReReviewRequest>()
            .eq(com.school.teaching.entity.ReReviewRequest::getStudentId, studentId)
            .orderByDesc(com.school.teaching.entity.ReReviewRequest::getCreatedAt));
    }
}
