package com.school.teaching.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ReportService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired private ExamMapper examMapper;
    @Autowired private ExamResultMapper resultMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;

    @Data @AllArgsConstructor
    public static class ScoreRow {
        @ExcelProperty("学号") private String studentNumber;
        @ExcelProperty("姓名") private String realName;
        @ExcelProperty("班级") private String className;
        @ExcelProperty("总分") private Integer totalScore;
        @ExcelProperty("正确题数") private Integer correctCount;
        @ExcelProperty("错误题数") private Integer wrongCount;
        @ExcelProperty("是否通过") private String isPassed;
    }

    @Override
    public byte[] exportExamScores(Long examId) {
        if (true) return new byte[0]; // 410 DISABLED — backup tables dropped
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException(404, "试卷不存在");

        List<ExamResult> results = resultMapper.selectList(
            new LambdaQueryWrapper<ExamResult>().eq(ExamResult::getExamId, examId)
                .in(ExamResult::getStatus, "submitted", "published"));

        Set<Long> sids = new HashSet<>();
        for (ExamResult r : results) sids.add(r.getStudentId());
        Map<Long, Student> sMap = sids.isEmpty() ? Map.of() : batchStudents(sids);
        Set<Long> uids = new HashSet<>();
        for (Student s : sMap.values()) uids.add(s.getUserId());
        Map<Long, User> uMap = uids.isEmpty() ? Map.of() : batchUsers(uids);
        Set<Long> cids = new HashSet<>();
        for (Student s : sMap.values()) if (s.getClassId() != null) cids.add(s.getClassId());
        Map<Long, Classes> cMap = cids.isEmpty() ? Map.of() : batchClasses(cids);

        List<ScoreRow> rows = new ArrayList<>();
        for (ExamResult r : results) {
            Student s = sMap.get(r.getStudentId());
            String sn = "", rn = "", cn = "";
            if (s != null) {
                sn = s.getStudentNumber() != null ? s.getStudentNumber() : "";
                User u = uMap.get(s.getUserId()); rn = u != null ? u.getRealName() : "";
                if (s.getClassId() != null) { Classes c = cMap.get(s.getClassId()); cn = c != null ? c.getClassName() : ""; }
            }
            rows.add(new ScoreRow(sn, rn, cn, r.getTotalScore(), r.getCorrectCount(), r.getWrongCount(), r.getIsPassed() == 1 ? "通过" : "未通过"));
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            EasyExcel.write(bos, ScoreRow.class).sheet("成绩").doWrite(rows);
            return bos.toByteArray();
        } catch (Exception e) { throw new BusinessException(500, "导出失败"); }
    }

    @Override
    public byte[] exportClassScores(Long classId) {
        if (true) return new byte[0]; // 410 DISABLED — backup tables dropped
        Classes cls = classesMapper.selectById(classId);
        if (cls == null) throw new BusinessException(404, "班级不存在");

        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, "published"));

        Set<Long> uids = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> uMap = batchUsers(uids);

        // 批量加载成绩: 按 examId+studentId 组合键
        Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        Map<String, Integer> scoreMap = new HashMap<>();
        if (!examIds.isEmpty() && !studentIds.isEmpty()) {
            resultMapper.selectList(new LambdaQueryWrapper<ExamResult>()
                .in(ExamResult::getExamId, examIds).in(ExamResult::getStudentId, studentIds))
                .forEach(r -> scoreMap.put(r.getExamId() + "_" + r.getStudentId(),
                    r.getTotalScore() != null ? r.getTotalScore() : 0));
        }

        List<List<Object>> body = new ArrayList<>();
        for (Student s : students) {
            User u = uMap.get(s.getUserId());
            List<Object> row = new ArrayList<>();
            row.add(s.getStudentNumber()); row.add(u != null ? u.getRealName() : "");
            for (Exam e : exams) {
                Integer score = scoreMap.get(e.getId() + "_" + s.getId());
                row.add(score != null ? score : "-");
            }
            body.add(row);
        }

        List<List<String>> headers = new ArrayList<>();
        headers.add(List.of("学号")); headers.add(List.of("姓名"));
        for (Exam e : exams) headers.add(List.of(e.getTitle()));

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            EasyExcel.write(bos).sheet("成绩汇总").head(headers).doWrite(body);
            return bos.toByteArray();
        } catch (Exception e) { throw new BusinessException(500, "导出失败"); }
    }

    private Map<Long, Student> batchStudents(Set<Long> ids) {
        Map<Long, Student> m = new HashMap<>();
        for (Student s : studentMapper.selectBatchIds(ids)) m.put(s.getId(), s);
        return m;
    }
    private Map<Long, User> batchUsers(Set<Long> ids) {
        Map<Long, User> m = new HashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) m.put(u.getId(), u);
        return m;
    }
    private Map<Long, Classes> batchClasses(Set<Long> ids) {
        Map<Long, Classes> m = new HashMap<>();
        for (Classes c : classesMapper.selectBatchIds(ids)) m.put(c.getId(), c);
        return m;
    }
}
