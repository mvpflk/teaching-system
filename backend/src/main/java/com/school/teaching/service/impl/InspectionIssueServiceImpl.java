package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.InspectionIssue;
import com.school.teaching.entity.InspectionRecord;
import com.school.teaching.entity.Notification;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.InspectionIssueMapper;
import com.school.teaching.mapper.InspectionRecordMapper;
import com.school.teaching.mapper.NotificationMapper;
import com.school.teaching.service.InspectionIssueCommentService;
import com.school.teaching.service.InspectionIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionIssueServiceImpl implements InspectionIssueService {

    private final InspectionIssueMapper mapper;
    private final InspectionRecordMapper recordMapper;
    private final NotificationMapper notificationMapper;
    private final InspectionIssueCommentService commentService;

    @Override
    public InspectionIssue getById(Long id) {
        InspectionIssue issue = mapper.selectById(id);
        if (issue == null) throw new BusinessException(404, "问题不存在");
        return issue;
    }

    @Override
    @Transactional
    public InspectionIssue create(InspectionIssue issue) {
        if (issue.getStatus() == null) issue.setStatus("OPEN");
        if (issue.getSeverity() == null) issue.setSeverity("MEDIUM");
        mapper.insert(issue);
        return issue;
    }

    @Override
    @Transactional
    public InspectionIssue update(Long id, InspectionIssue data) {
        InspectionIssue existing = getById(id);
        if (data.getRecordId() != null) existing.setRecordId(data.getRecordId());
        if (data.getTitle() != null) existing.setTitle(data.getTitle());
        if (data.getDescription() != null) existing.setDescription(data.getDescription());
        if (data.getCategory() != null) existing.setCategory(data.getCategory());
        if (data.getSeverity() != null) existing.setSeverity(data.getSeverity());
        if (data.getStatus() != null) existing.setStatus(data.getStatus());
        if (data.getAssignedTo() != null) existing.setAssignedTo(data.getAssignedTo());
        if (data.getAssignedClassId() != null) existing.setAssignedClassId(data.getAssignedClassId());
        if (data.getRelatedTaskId() != null) existing.setRelatedTaskId(data.getRelatedTaskId());
        if (data.getDeadline() != null) existing.setDeadline(data.getDeadline());
        if (data.getAttachmentUrls() != null) existing.setAttachmentUrls(data.getAttachmentUrls());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        InspectionIssue issue = getById(id);
        if (!"OPEN".equals(issue.getStatus())) {
            throw new BusinessException(409, "仅待处理状态的问题可删除");
        }
        mapper.deleteById(id);
    }

    @Override
    public IPage<InspectionIssue> getPage(String status, String category, String severity,
                                          Long assignedTo, Long assignedClassId, Long createdBy,
                                          String startDate, String endDate,
                                          int page, int size) {
        LambdaQueryWrapper<InspectionIssue> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) q.eq(InspectionIssue::getStatus, status);
        if (category != null && !category.isEmpty()) q.eq(InspectionIssue::getCategory, category);
        if (severity != null && !severity.isEmpty()) q.eq(InspectionIssue::getSeverity, severity);
        if (assignedTo != null) q.eq(InspectionIssue::getAssignedTo, assignedTo);
        if (assignedClassId != null) q.eq(InspectionIssue::getAssignedClassId, assignedClassId);
        if (createdBy != null) q.eq(InspectionIssue::getCreatedBy, createdBy);
        if (startDate != null && !startDate.isEmpty()) q.ge(InspectionIssue::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        if (endDate != null && !endDate.isEmpty()) q.le(InspectionIssue::getCreatedAt, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        q.orderByDesc(InspectionIssue::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    @Transactional
    public InspectionIssue startProgress(Long issueId, Long teacherId) {
        InspectionIssue issue = getById(issueId);
        if (!"ASSIGNED".equals(issue.getStatus())) {
            throw new BusinessException(409, "仅已指派状态的问题可开始处理");
        }
        if (!teacherId.equals(issue.getAssignedTo())) {
            throw new BusinessException(403, "仅被指派的教师可开始处理");
        }
        issue.setStatus("IN_PROGRESS");
        mapper.updateById(issue);
        commentService.addComment(issueId, teacherId, "教师已开始处理该问题", 1);
        return issue;
    }

    @Override
    @Transactional
    public InspectionIssue assignIssue(Long issueId, Long teacherId, String deadline) {
        InspectionIssue issue = getById(issueId);
        if (!"OPEN".equals(issue.getStatus())) {
            throw new BusinessException(409, "仅待处理状态的问题可指派");
        }
        issue.setStatus("ASSIGNED");
        issue.setAssignedTo(teacherId);
        if (deadline != null && !deadline.isEmpty()) issue.setDeadline(LocalDate.parse(deadline));
        mapper.updateById(issue);

        commentService.addComment(issueId, issue.getCreatedBy(), "巡视员已将问题指派给教师，整改截止日期：" + (deadline != null ? deadline : "未设置"), 1);

        Notification n = new Notification();
        n.setUserId(issue.getAssignedTo());
        n.setTitle("您有一个新的整改任务");
        n.setContent("巡视员指派了问题【" + issue.getTitle() + "】给您，请在" + (deadline != null ? deadline : "尽快") + "前完成整改");
        n.setType("INSPECTION");
        n.setIsRead(0);
        n.setRelatedId(issueId);
        notificationMapper.insert(n);

        return issue;
    }

    @Override
    @Transactional
    public InspectionIssue resolveIssue(Long issueId, Long teacherId, String resolveComment) {
        InspectionIssue issue = getById(issueId);
        if (!"ASSIGNED".equals(issue.getStatus()) && !"IN_PROGRESS".equals(issue.getStatus())) {
            throw new BusinessException(409, "仅已指派或处理中的问题可提交整改结果");
        }
        issue.setStatus("RESOLVED");
        issue.setResolvedBy(teacherId);
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolveComment(resolveComment);
        mapper.updateById(issue);

        commentService.addComment(issueId, teacherId, "教师已提交整改结果：" + (resolveComment != null ? resolveComment : ""), 1);

        Notification n = new Notification();
        n.setUserId(issue.getCreatedBy());
        n.setTitle("整改结果待验收");
        n.setContent("教师已提交问题【" + issue.getTitle() + "】的整改结果，请及时验收");
        n.setType("INSPECTION");
        n.setIsRead(0);
        n.setRelatedId(issueId);
        notificationMapper.insert(n);

        return issue;
    }

    @Override
    @Transactional
    public InspectionIssue verifyIssue(Long issueId, Long inspectorId, boolean approved, String verifyComment) {
        InspectionIssue issue = getById(issueId);
        if (!"RESOLVED".equals(issue.getStatus())) {
            throw new BusinessException(409, "仅已解决状态的问题可验收");
        }
        if (approved) {
            issue.setStatus("VERIFIED");
        } else {
            issue.setStatus("REJECTED");
        }
        issue.setVerifiedBy(inspectorId);
        issue.setVerifiedAt(LocalDateTime.now());
        issue.setVerifyComment(verifyComment);
        mapper.updateById(issue);

        commentService.addComment(issueId, inspectorId,
            (approved ? "巡视员已验收通过" : "巡视员已驳回，需重新整改") +
            (verifyComment != null && !verifyComment.isEmpty() ? "：" + verifyComment : ""), 1);

        Notification n = new Notification();
        n.setUserId(issue.getAssignedTo());
        n.setTitle(approved ? "整改已验收通过" : "整改被驳回");
        n.setContent(approved ? "问题【" + issue.getTitle() + "】整改已通过验收"
            : "问题【" + issue.getTitle() + "】整改被驳回，请重新整改：" + (verifyComment != null ? verifyComment : ""));
        n.setType("INSPECTION");
        n.setIsRead(0);
        n.setRelatedId(issueId);
        notificationMapper.insert(n);

        if (approved && issue.getRecordId() != null) {
            checkAndArchiveRecord(issue.getRecordId());
        }

        return issue;
    }

    private void checkAndArchiveRecord(Long recordId) {
        Long total = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>()
            .eq(InspectionIssue::getRecordId, recordId));
        Long verified = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>()
            .eq(InspectionIssue::getRecordId, recordId)
            .eq(InspectionIssue::getStatus, "VERIFIED"));
        if (total != null && verified != null && total.equals(verified)) {
            InspectionRecord record = recordMapper.selectById(recordId);
            if (record != null) {
                record.setStatus("ARCHIVED");
                recordMapper.updateById(record);
            }
        }
    }

    @Override
    public Map<String, Object> getIssueStats() {
        long total = mapper.selectCount(null);
        long openCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>().eq(InspectionIssue::getStatus, "OPEN"));
        long assignedCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>().eq(InspectionIssue::getStatus, "ASSIGNED"));
        long inProgressCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>().eq(InspectionIssue::getStatus, "IN_PROGRESS"));
        long resolvedCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>().eq(InspectionIssue::getStatus, "RESOLVED"));
        long verifiedCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>().eq(InspectionIssue::getStatus, "VERIFIED"));
        long overdueCount = mapper.selectCount(new LambdaQueryWrapper<InspectionIssue>()
            .in(InspectionIssue::getStatus, "ASSIGNED", "IN_PROGRESS")
            .isNotNull(InspectionIssue::getDeadline)
            .lt(InspectionIssue::getDeadline, LocalDate.now()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("openCount", openCount);
        stats.put("assignedCount", assignedCount);
        stats.put("inProgressCount", inProgressCount);
        stats.put("resolvedCount", resolvedCount);
        stats.put("verifiedCount", verifiedCount);
        stats.put("overdueCount", overdueCount);
        return stats;
    }

    @Override
    public IPage<InspectionIssue> getMyAssignedIssues(Long teacherId, String status, int page, int size) {
        LambdaQueryWrapper<InspectionIssue> q = new LambdaQueryWrapper<InspectionIssue>()
            .eq(InspectionIssue::getAssignedTo, teacherId);
        if (status != null && !status.isEmpty()) q.eq(InspectionIssue::getStatus, status);
        q.orderByDesc(InspectionIssue::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public InspectionIssueCommentService getCommentService() {
        return commentService;
    }
}
