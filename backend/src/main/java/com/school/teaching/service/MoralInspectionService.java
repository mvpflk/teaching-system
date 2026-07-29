package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.MoralInspection;

import java.util.List;

public interface MoralInspectionService {
    MoralInspection getById(Long id);
    MoralInspection create(MoralInspection inspection);
    MoralInspection update(Long id, MoralInspection inspection);
    void delete(Long id);
    IPage<MoralInspection> getPage(Long classId, Long inspectorId, String category,
                                   String startDate, String endDate,
                                   int page, int size);
    List<MoralInspection> getRecentInspections(Long classId, int limit);
}
