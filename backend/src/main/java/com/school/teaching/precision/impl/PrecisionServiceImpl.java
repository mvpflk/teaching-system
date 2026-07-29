package com.school.teaching.precision.impl;

import com.school.teaching.precision.PrecisionEnglishService;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.precision.PrecisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PrecisionServiceImpl implements PrecisionService {

    @Autowired
    private PrecisionHelper precisionHelper;

    @Autowired
    private PrecisionDiagnosisService diagnosisService;

    @Autowired
    private PrecisionTeacherService teacherService;

    @Autowired
    private PrecisionStudyService studyService;

    @Autowired
    private PrecisionReportService reportService;

    @Override
    public Map<String, Object> getDashboard(Long studentId) {
        return diagnosisService.getDashboard(studentId);
    }

    @Override
    public Map<String, Object> getDiagnosis(Long studentId, String subject) {
        return diagnosisService.getDiagnosis(studentId, subject);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> submitDiagnosis(Long studentId, String subject, List<Map<String, Object>> answers) {
        return diagnosisService.submitDiagnosis(studentId, subject, answers);
    }

    @Override
    public Map<String, Object> gradeOneAnswer(Long studentId, Long questionId, String answer,
                                               String subject, String questionType) {
        return diagnosisService.gradeOneAnswer(studentId, questionId, answer, subject, questionType);
    }

    @Override
    public String getWeeklyPackHtml(Long studentId, String subject, int weekNo) {
        return studyService.getWeeklyPackHtml(studentId, subject, weekNo);
    }

    @Override
    public List<Map<String, Object>> getPackQuestions(Long studentId, String subject) {
        return studyService.getPackQuestions(studentId, subject);
    }

    @Override
    public Map<String, Object> getOnlineTest(Long studentId, String subject) {
        return studyService.getOnlineTest(studentId, subject);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> submitOnlineTest(Long studentId, Map<String, Object> body) {
        return studyService.submitOnlineTest(studentId, body);
    }

    @Override
    public Map<String, Object> getReport(Long studentId, String subject) {
        return reportService.getReport(studentId, subject);
    }

    @Override
    public List<Map<String, Object>> getSyllabusMap(Long studentId, String subject) {
        return reportService.getSyllabusMap(studentId, subject);
    }

    @Override
    public List<Map<String, Object>> getPracticeQuestions(Long studentId, Long nodeId, String subject) {
        return reportService.getPracticeQuestions(studentId, nodeId, subject);
    }

    @Override
    public Map<String, Object> aiQa(Long studentId, String question) {
        return reportService.aiQa(studentId, question);
    }

    @Override
    public Map<String, Object> teacherOverview(Long teacherUserId) {
        return teacherService.teacherOverview(teacherUserId);
    }

    @Override
    public List<Map<String, Object>> teacherStudents(Long teacherUserId, Long groupId, String subject) {
        return teacherService.teacherStudents(teacherUserId, groupId, subject);
    }

    @Override
    public int remindAll(Long teacherUserId, String subject) {
        return teacherService.remindAll(teacherUserId, subject);
    }

    @Override
    @Transactional
    public Map<String, Object> composeRemedialTask(Long teacherUserId, Long groupId, Long classId, String subject) {
        return teacherService.composeRemedialTask(teacherUserId, groupId, classId, subject);
    }

    @Override
    public List<Map<String, Object>> teacherWeakTop(Long teacherUserId, String subject, int topN) {
        return teacherService.teacherWeakTop(teacherUserId, subject, topN);
    }

    @Override
    public boolean remindStudent(Long teacherUserId, Long studentId, String subject) {
        return teacherService.remindStudent(teacherUserId, studentId, subject);
    }

    @Override
    public void assertTeacherOwnsStudent(Long teacherUserId, Long studentId) {
        teacherService.assertTeacherOwnsStudent(teacherUserId, studentId);
    }

    @Override
    public void assertTeacherOwnsClass(Long teacherUserId, Long classId) {
        teacherService.assertTeacherOwnsClass(teacherUserId, classId);
    }

    @Override
    public Map<String, Object> getClassWeaknesses(Long classId) {
        return teacherService.getClassWeaknesses(classId);
    }

    @Override
    public Map<String, Object> getStudentKpStatus(Long studentId, Long kpId) {
        return reportService.getStudentKpStatus(studentId, kpId);
    }

    @Override
    public Map<String, Object> ensureFromQuality(Long studentId, Long kpId, String subject) {
        return reportService.ensureFromQuality(studentId, kpId, subject);
    }

    @Override
    public Map<String, Object> uploadAnswerPhoto(Long studentId, Long questionId,
            String questionType, MultipartFile file) {
        return reportService.uploadAnswerPhoto(studentId, questionId, questionType, file);
    }

}
