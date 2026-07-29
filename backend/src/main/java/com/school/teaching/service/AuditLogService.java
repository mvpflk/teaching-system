package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.AuditLog;

public interface AuditLogService {

    /** 异步保存审计日志 */
    void asyncSave(AuditLog log);

    /** 分页查询审计日志 */
    IPage<AuditLog> page(Page<AuditLog> page, String eventType, String username, String startTime, String endTime);

    /** 事件类型分布统计 */
    java.util.List<java.util.Map<String, Object>> getEventDistribution();

    /** 活跃用户排行 */
    java.util.List<java.util.Map<String, Object>> getActiveUsers();

    /** 小时趋势统计 */
    java.util.List<java.util.Map<String, Object>> getHourlyTrend();
}
