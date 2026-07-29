package com.school.teaching.service;

public interface DraftService {
    void save(Long studentId, Long taskId, String content);
    String load(Long studentId, Long taskId);
    void delete(Long studentId, Long taskId);
}
