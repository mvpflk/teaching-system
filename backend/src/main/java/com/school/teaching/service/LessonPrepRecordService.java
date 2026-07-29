package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.LessonPrepRecord;

import java.util.List;
import java.util.Map;

public interface LessonPrepRecordService {
    LessonPrepRecord getById(Long id);
    LessonPrepRecord create(LessonPrepRecord record);
    LessonPrepRecord update(Long id, LessonPrepRecord data);
    void delete(Long id);
    IPage<LessonPrepRecord> getPage(Long groupId, String startDate, String endDate, int page, int size);
    List<Map<String, Object>> getGroupStats(Long groupId);
}
