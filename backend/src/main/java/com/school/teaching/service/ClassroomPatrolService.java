package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.ClassroomPatrol;

import java.util.List;

public interface ClassroomPatrolService {
    ClassroomPatrol getById(Long id);
    ClassroomPatrol create(ClassroomPatrol patrol);
    ClassroomPatrol update(Long id, ClassroomPatrol patrol);
    void delete(Long id);
    IPage<ClassroomPatrol> getPage(Long classId, Long teacherId, Long inspectorId,
                                   String subject, String startDate, String endDate,
                                   int page, int size);
    List<ClassroomPatrol> getRecentPatrols(Long classId, int limit);
}
