package com.school.teaching.service;

public interface ReportService {
    byte[] exportExamScores(Long examId);
    byte[] exportClassScores(Long classId);
}
