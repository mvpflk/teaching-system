package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.TeachingResearchActivity;

public interface TeachingResearchActivityService {
    TeachingResearchActivity create(TeachingResearchActivity activity);
    TeachingResearchActivity getById(Long id);
    TeachingResearchActivity update(Long id, TeachingResearchActivity data);
    void delete(Long id);
    IPage<TeachingResearchActivity> getPage(Long teachingGroupId, String activityType,
        String startDate, String endDate, int page, int size);
}
