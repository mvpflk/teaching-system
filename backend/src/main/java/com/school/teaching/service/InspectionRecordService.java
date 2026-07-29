package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.InspectionRecord;

public interface InspectionRecordService {
    InspectionRecord getById(Long id);
    InspectionRecord create(InspectionRecord record);
    InspectionRecord update(Long id, InspectionRecord record);
    void delete(Long id);
    IPage<InspectionRecord> getPage(Long inspectorId, String recordType,
                                    String startDate, String endDate,
                                    int page, int size);
}
