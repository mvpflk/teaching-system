package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.TeacherActivity;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.TeacherActivityMapper;
import com.school.teaching.service.TeacherActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class TeacherActivityServiceImpl implements TeacherActivityService {

    @Autowired
    private TeacherActivityMapper activityMapper;

    @Override
    public void log(Long teacherId, String action, String targetType, Long targetId) {
        TeacherActivity a = new TeacherActivity();
        a.setTeacherId(teacherId);
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        try {
            activityMapper.insert(a);
        } catch (Exception e) {
            log.error("录入教师行为日志失败 teacherId={} action={}: {}", teacherId, action, e.getMessage(), e);
            throw new BusinessException(500, "录入教师行为日志失败: " + e.getMessage());
        }
    }

    @Override
    public List<TeacherActivity> listByTeacher(Long teacherId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return activityMapper.selectList(new LambdaQueryWrapper<TeacherActivity>()
            .eq(TeacherActivity::getTeacherId, teacherId)
            .orderByDesc(TeacherActivity::getCreatedAt)
            .last("LIMIT " + safeLimit));
    }

    @Override
    public boolean hasCheckedToday(Long teacherId) {
        return getTodayCheck(teacherId) != null;
    }

    @Override
    public Boolean getTodayCheck(Long teacherId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        List<TeacherActivity> checks = activityMapper.selectList(new LambdaQueryWrapper<TeacherActivity>()
            .eq(TeacherActivity::getTeacherId, teacherId)
            .eq(TeacherActivity::getAction, "PENETRATION_CHECK")
            .between(TeacherActivity::getCreatedAt, todayStart, todayEnd)
            .orderByDesc(TeacherActivity::getCreatedAt)
            .last("LIMIT 1"));
        if (checks.isEmpty()) return null;
        return "yes".equals(checks.get(0).getTargetType());
    }

    @Override
    public List<TeacherActivity> listPenetrationChecks(Long teacherId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return activityMapper.selectList(new LambdaQueryWrapper<TeacherActivity>()
            .eq(TeacherActivity::getTeacherId, teacherId)
            .eq(TeacherActivity::getAction, "PENETRATION_CHECK")
            .orderByDesc(TeacherActivity::getCreatedAt)
            .last("LIMIT " + safeLimit));
    }
}
