package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.TeachingResearchActivity;
import com.school.teaching.mapper.TeachingResearchActivityMapper;
import com.school.teaching.service.TeachingResearchActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeachingResearchActivityServiceImpl implements TeachingResearchActivityService {

    private final TeachingResearchActivityMapper mapper;

    @Override
    public TeachingResearchActivity create(TeachingResearchActivity activity) {
        mapper.insert(activity);
        return activity;
    }

    @Override
    public TeachingResearchActivity getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public TeachingResearchActivity update(Long id, TeachingResearchActivity data) {
        data.setId(id);
        mapper.updateById(data);
        return mapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public IPage<TeachingResearchActivity> getPage(Long teachingGroupId, String activityType,
            String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<TeachingResearchActivity> qw = new LambdaQueryWrapper<>();
        if (teachingGroupId != null) qw.eq(TeachingResearchActivity::getTeachingGroupId, teachingGroupId);
        if (activityType != null && !activityType.isEmpty()) qw.eq(TeachingResearchActivity::getActivityType, activityType);
        qw.orderByDesc(TeachingResearchActivity::getActivityDate);
        return mapper.selectPage(new Page<>(page, size), qw);
    }
}
