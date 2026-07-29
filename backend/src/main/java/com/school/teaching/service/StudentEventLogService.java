package com.school.teaching.service;

import com.school.teaching.entity.StudentEventLogEntity;
import com.school.teaching.mapper.StudentEventLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEventLogService {

    private final StudentEventLogMapper eventLogMapper;

    /** 记录事件。异常静默，不阻塞主流程 */
    public void log(Long studentId, String eventType, Map<String, Object> data, String sourceModule) {
        try {
            StudentEventLogEntity e = new StudentEventLogEntity();
            e.setStudentId(studentId);
            e.setSchoolId(1L);
            e.setStageId(4L);
            e.setEventType(eventType);
            e.setEventData(toJson(data));
            e.setSourceModule(sourceModule);
            e.setOccurredAt(LocalDateTime.now());
            eventLogMapper.insert(e);
        } catch (Exception e) {
            log.warn("事件日志记录失败: type={}, studentId={}", eventType, studentId, e);
        }
    }

    private String toJson(Map<String, Object> data) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data); }
        catch (Exception e) { return "{}"; }
    }
}
