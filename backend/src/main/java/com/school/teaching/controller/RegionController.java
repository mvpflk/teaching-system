package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 区域聚合 API — 预留，当前返回 501。
 * 后续实现：从各校 DB 定时拉取统计数据，提供区域级别的分析视图。
 */
@RestController
@RequestMapping("/region")
@PreAuthorize("hasAnyRole('REGION_ADMIN','ADMIN','SUPER_ADMIN')")
public class RegionController {

    /** 区域仪表盘 — 辖区内所有学校的概览数据 */
    @GetMapping("/actions/dashboard")
    public R<Map<String, Object>> dashboard() {
        if (!canAccess()) return R.error(403, "无区域管理权限");
        return R.error(501, "区域仪表盘暂未开放");
    }

    /** 跨校对比 — 同年级/同学科/同学段的多校数据对比 */
    @GetMapping("/actions/schools/compare")
    public R<Map<String, Object>> compareSchools(@RequestParam(required = false) String grade,
                                                  @RequestParam(required = false) String subject,
                                                  @RequestParam(required = false) Long stageId) {
        if (!canAccess()) return R.error(403, "无区域管理权限");
        return R.error(501, "跨校对比暂未开放");
    }

    /** 区域考试分析 — 全区域考试数据汇总 */
    @GetMapping("/actions/analytics/exam")
    public R<Map<String, Object>> examAnalytics(@RequestParam(required = false) Long examId,
                                                 @RequestParam(required = false) Long schoolId) {
        if (!canAccess()) return R.error(403, "无区域管理权限");
        return R.error(501, "区域考试分析暂未开放");
    }

    private boolean canAccess() {
        return SecurityUtils.isRegionAdmin() || SecurityUtils.isSuperAdmin();
    }
}
