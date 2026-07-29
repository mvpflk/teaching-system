package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.AuditLog;
import com.school.teaching.mapper.AuditLogMapper;
import com.school.teaching.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Override
    @Async
    public void asyncSave(AuditLog log) {
        auditLogMapper.insert(log);
    }

    @Override
    public IPage<AuditLog> page(Page<AuditLog> page, String eventType, String username, String startTime, String endTime) {
        var q = new LambdaQueryWrapper<AuditLog>()
            .orderByDesc(AuditLog::getCreatedTime);
        if (eventType != null && !eventType.isEmpty()) q.eq(AuditLog::getEventType, eventType);
        if (username != null && !username.isEmpty()) q.like(AuditLog::getUsername, username);
        if (startTime != null && !startTime.isEmpty())
            q.ge(AuditLog::getCreatedTime, java.time.LocalDateTime.parse(startTime));
        if (endTime != null && !endTime.isEmpty())
            q.le(AuditLog::getCreatedTime, java.time.LocalDateTime.parse(endTime));
        return auditLogMapper.selectPage(page, q);
    }

    @Override
    public List<Map<String, Object>> getEventDistribution() {
        return auditLogMapper.selectMaps(new QueryWrapper<AuditLog>()
            .select("event_type as name, COUNT(*) as value")
            .groupBy("event_type")
            .orderByDesc("value"));
    }

    @Override
    public List<Map<String, Object>> getActiveUsers() {
        return auditLogMapper.selectMaps(new QueryWrapper<AuditLog>()
            .select("username as name, COUNT(*) as value")
            .groupBy("username")
            .orderByDesc("value")
            .last("LIMIT 20"));
    }

    @Override
    public List<Map<String, Object>> getHourlyTrend() {
        return auditLogMapper.selectMaps(new QueryWrapper<AuditLog>()
            .select("DATE_FORMAT(created_time, '%Y-%m-%d %H:00') as name, COUNT(*) as value")
            .groupBy("name")
            .orderByAsc("name")
            .last("LIMIT 168"));
    }
}
