package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.ClassroomPatrol;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassroomPatrolMapper;
import com.school.teaching.service.ClassroomPatrolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomPatrolServiceImpl implements ClassroomPatrolService {

    private final ClassroomPatrolMapper mapper;

    @Override
    public ClassroomPatrol getById(Long id) {
        ClassroomPatrol r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "巡课记录不存在");
        return r;
    }

    @Override
    @Transactional
    public ClassroomPatrol create(ClassroomPatrol patrol) {
        mapper.insert(patrol);
        return patrol;
    }

    @Override
    @Transactional
    public ClassroomPatrol update(Long id, ClassroomPatrol data) {
        ClassroomPatrol existing = getById(id);
        if (data.getClassId() != null) existing.setClassId(data.getClassId());
        if (data.getTeacherId() != null) existing.setTeacherId(data.getTeacherId());
        if (data.getInspectorId() != null) existing.setInspectorId(data.getInspectorId());
        if (data.getSubject() != null) existing.setSubject(data.getSubject());
        if (data.getPatrolDate() != null) existing.setPatrolDate(data.getPatrolDate());
        if (data.getPeriod() != null) existing.setPeriod(data.getPeriod());
        if (data.getDisciplineScore() != null) existing.setDisciplineScore(data.getDisciplineScore());
        if (data.getTeachingScore() != null) existing.setTeachingScore(data.getTeachingScore());
        if (data.getInteractionScore() != null) existing.setInteractionScore(data.getInteractionScore());
        if (data.getNote() != null) existing.setNote(data.getNote());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Override
    public IPage<ClassroomPatrol> getPage(Long classId, Long teacherId, Long inspectorId,
                                          String subject, String startDate, String endDate,
                                          int page, int size) {
        LambdaQueryWrapper<ClassroomPatrol> q = new LambdaQueryWrapper<>();
        if (classId != null) q.eq(ClassroomPatrol::getClassId, classId);
        if (teacherId != null) q.eq(ClassroomPatrol::getTeacherId, teacherId);
        if (inspectorId != null) q.eq(ClassroomPatrol::getInspectorId, inspectorId);
        if (subject != null && !subject.isEmpty()) q.eq(ClassroomPatrol::getSubject, subject);
        if (startDate != null && !startDate.isEmpty()) q.ge(ClassroomPatrol::getPatrolDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty()) q.le(ClassroomPatrol::getPatrolDate, LocalDate.parse(endDate));
        q.orderByDesc(ClassroomPatrol::getPatrolDate);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public List<ClassroomPatrol> getRecentPatrols(Long classId, int limit) {
        LambdaQueryWrapper<ClassroomPatrol> q = new LambdaQueryWrapper<>();
        q.eq(ClassroomPatrol::getClassId, classId);
        q.orderByDesc(ClassroomPatrol::getPatrolDate);
        q.last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return mapper.selectList(q);
    }
}
