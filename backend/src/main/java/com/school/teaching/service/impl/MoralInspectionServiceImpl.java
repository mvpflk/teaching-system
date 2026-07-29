package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.MoralInspection;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.MoralInspectionMapper;
import com.school.teaching.service.MoralInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoralInspectionServiceImpl implements MoralInspectionService {

    private final MoralInspectionMapper mapper;

    @Override
    public MoralInspection getById(Long id) {
        MoralInspection r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "德育检查记录不存在");
        return r;
    }

    @Override
    @Transactional
    public MoralInspection create(MoralInspection inspection) {
        mapper.insert(inspection);
        return inspection;
    }

    @Override
    @Transactional
    public MoralInspection update(Long id, MoralInspection data) {
        MoralInspection existing = getById(id);
        if (data.getClassId() != null) existing.setClassId(data.getClassId());
        if (data.getInspectorId() != null) existing.setInspectorId(data.getInspectorId());
        if (data.getInspectionDate() != null) existing.setInspectionDate(data.getInspectionDate());
        if (data.getCategory() != null) existing.setCategory(data.getCategory());
        if (data.getScore() != null) existing.setScore(data.getScore());
        if (data.getDescription() != null) existing.setDescription(data.getDescription());
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
    public IPage<MoralInspection> getPage(Long classId, Long inspectorId, String category,
                                          String startDate, String endDate,
                                          int page, int size) {
        LambdaQueryWrapper<MoralInspection> q = new LambdaQueryWrapper<>();
        if (classId != null) q.eq(MoralInspection::getClassId, classId);
        if (inspectorId != null) q.eq(MoralInspection::getInspectorId, inspectorId);
        if (category != null && !category.isEmpty()) q.eq(MoralInspection::getCategory, category);
        if (startDate != null && !startDate.isEmpty()) q.ge(MoralInspection::getInspectionDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty()) q.le(MoralInspection::getInspectionDate, LocalDate.parse(endDate));
        q.orderByDesc(MoralInspection::getInspectionDate);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public List<MoralInspection> getRecentInspections(Long classId, int limit) {
        LambdaQueryWrapper<MoralInspection> q = new LambdaQueryWrapper<>();
        q.eq(MoralInspection::getClassId, classId);
        q.orderByDesc(MoralInspection::getInspectionDate);
        q.last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return mapper.selectList(q);
    }
}
