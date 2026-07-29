package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.InspectionIssue;
import com.school.teaching.entity.InspectionReport;
import com.school.teaching.entity.RectificationNotice;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.InspectionIssueMapper;
import com.school.teaching.mapper.InspectionReportMapper;
import com.school.teaching.mapper.RectificationNoticeMapper;
import com.school.teaching.service.InspectionReportService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionReportServiceImpl implements InspectionReportService {

    private final InspectionReportMapper mapper;
    private final InspectionIssueMapper issueMapper;
    private final RectificationNoticeMapper noticeMapper;

    @Override
    public InspectionReport getById(Long id) {
        InspectionReport r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "巡视报告不存在");
        return r;
    }

    @Override
    @Transactional
    public InspectionReport generate(String reportType, LocalDate periodStart, LocalDate periodEnd, Long userId) {
        LambdaQueryWrapper<InspectionIssue> issueQ = new LambdaQueryWrapper<>();
        if (periodStart != null) issueQ.ge(InspectionIssue::getCreatedAt, periodStart.atStartOfDay());
        if (periodEnd != null) issueQ.le(InspectionIssue::getCreatedAt, periodEnd.plusDays(1).atStartOfDay());
        long issueCount = issueMapper.selectCount(issueQ);

        LambdaQueryWrapper<InspectionIssue> resolvedQ = new LambdaQueryWrapper<>();
        if (periodStart != null) resolvedQ.ge(InspectionIssue::getResolvedAt, periodStart.atStartOfDay());
        if (periodEnd != null) resolvedQ.le(InspectionIssue::getResolvedAt, periodEnd.plusDays(1).atStartOfDay());
        resolvedQ.in(InspectionIssue::getStatus, "RESOLVED", "VERIFIED");
        long resolvedCount = issueMapper.selectCount(resolvedQ);

        long overdueCount = issueMapper.selectCount(new LambdaQueryWrapper<InspectionIssue>()
            .in(InspectionIssue::getStatus, "ASSIGNED", "IN_PROGRESS")
            .isNotNull(InspectionIssue::getDeadline)
            .lt(InspectionIssue::getDeadline, LocalDate.now()));

        LambdaQueryWrapper<RectificationNotice> noticeQ = new LambdaQueryWrapper<>();
        if (periodStart != null) noticeQ.ge(RectificationNotice::getSentAt, periodStart.atStartOfDay());
        if (periodEnd != null) noticeQ.le(RectificationNotice::getSentAt, periodEnd.plusDays(1).atStartOfDay());
        long noticeCount = noticeMapper.selectCount(noticeQ);

        Map<String, Object> summary = new HashMap<>();
        summary.put("issueCount", issueCount);
        summary.put("resolvedCount", resolvedCount);
        summary.put("overdueCount", overdueCount);
        summary.put("noticeCount", noticeCount);
        summary.put("resolutionRate", issueCount > 0 ? (double) resolvedCount / issueCount : 0);

        InspectionReport report = new InspectionReport();
        report.setReportType(reportType);
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setIssueCount((int) issueCount);
        report.setResolvedCount((int) resolvedCount);
        report.setNoticeCount((int) noticeCount);
        report.setSummaryJson(JsonUtils.toJson(summary));
        report.setStatus("GENERATED");
        report.setGeneratedBy(userId);
        Map<String, String> titleMap = Map.of(
            "WEEKLY", "周巡视报告",
            "MONTHLY", "月巡视报告",
            "SEMESTER", "学期巡视报告",
            "AD_HOC", "专项巡视报告"
        );
        report.setTitle(titleMap.getOrDefault(reportType, reportType + "巡视报告"));
        mapper.insert(report);
        return report;
    }

    @Override
    @Transactional
    public InspectionReport publish(Long id) {
        InspectionReport r = getById(id);
        if (!"GENERATED".equals(r.getStatus())) {
            throw new BusinessException(409, "仅已生成状态的报告可发布");
        }
        r.setStatus("PUBLISHED");
        mapper.updateById(r);
        return r;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        InspectionReport r = getById(id);
        if ("PUBLISHED".equals(r.getStatus())) {
            throw new BusinessException(409, "已发布的报告不可删除");
        }
        mapper.deleteById(id);
    }

    @Override
    public IPage<InspectionReport> getPage(int page, int size) {
        LambdaQueryWrapper<InspectionReport> q = new LambdaQueryWrapper<>();
        q.orderByDesc(InspectionReport::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }
}
