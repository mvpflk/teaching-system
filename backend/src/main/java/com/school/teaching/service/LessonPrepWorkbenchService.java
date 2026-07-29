package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.LessonPrepGroup;
import com.school.teaching.entity.LessonPrepRecord;

import java.util.List;
import java.util.Map;

public interface LessonPrepWorkbenchService {
    LessonPrepGroup getMyLessonPrepGroup(Long teacherId);
    IPage<LessonPrepRecord> getRecords(Long groupId, String startDate, String endDate, int page, int size);
    LessonPrepRecord createRecord(LessonPrepRecord record, Long teacherId, Long groupId);
    LessonPrepRecord updateRecord(Long id, LessonPrepRecord data, Long teacherId);
    void deleteRecord(Long id, Long teacherId);
    List<Map<String, Object>> getMembers(Long groupId);
    List<Map<String, Object>> getPendingReviews(Long groupId);
}
