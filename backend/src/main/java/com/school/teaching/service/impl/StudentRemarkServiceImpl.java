package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.StudentRemarkService;
import com.school.teaching.utils.ScoreUtils;
import com.school.teaching.service.StudentTimelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentRemarkServiceImpl implements StudentRemarkService {

    @Autowired private StudentRemarkMapper remarkMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private com.school.teaching.mapper.TaskSubmissionMapper submissionMapper;
    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private com.school.teaching.mapper.CreditTransactionMapper creditMapper;
    @Autowired private StudentTimelineService timelineService;

    private static String currentSemester() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        if (month >= 2 && month <= 7) return year + "-" + (year + 1) + "-2";
        else if (month >= 9) return year + "-" + (year + 1) + "-1";
        else return (year - 1) + "-" + year + "-1";
    }

    @Override
    public List<Map<String, Object>> studentsWithRemarks(Long classId) {
        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        Set<Long> userIds = students.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> sids = students.stream().map(Student::getId).collect(Collectors.toSet());
        String sem = currentSemester();
        Map<Long, String> remarkMap = remarkMapper.selectList(
            new LambdaQueryWrapper<StudentRemark>().in(StudentRemark::getStudentId, sids).eq(StudentRemark::getSemester, sem))
            .stream().collect(Collectors.toMap(StudentRemark::getStudentId, StudentRemark::getRemark));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> m = new HashMap<>();
            User u = userMap.get(s.getUserId());
            m.put("studentId", s.getId());
            m.put("name", u != null ? u.getRealName() : "");
            m.put("studentNumber", s.getStudentNumber());
            m.put("remark", remarkMap.getOrDefault(s.getId(), ""));
            result.add(m);
        }
        return result;
    }

    @Override
    public void updateRemark(Long studentId, String remark) {
        String sem = currentSemester();
        StudentRemark existing = remarkMapper.selectOne(
            new LambdaQueryWrapper<StudentRemark>()
                .eq(StudentRemark::getStudentId, studentId).eq(StudentRemark::getSemester, sem));
        if (existing != null) {
            existing.setRemark(remark);
            remarkMapper.updateById(existing);
        } else {
            StudentRemark r = new StudentRemark();
            r.setStudentId(studentId);
            r.setSemester(sem);
            r.setRemark(remark);
            remarkMapper.insert(r);
        }
    }

    @Override
    public Map<String, Object> growthReport(Long userId) {
        Student s = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (s == null) return null;
        User u = userMapper.selectById(userId);
        Classes cls = s.getClassId() != null ? classesMapper.selectById(s.getClassId()) : null;

        Map<String, Object> report = new LinkedHashMap<>();

        // 基本信息
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", u != null ? u.getRealName() : "");
        info.put("studentNumber", s.getStudentNumber());
        info.put("className", cls != null ? cls.getClassName() : "");
        info.put("grade", cls != null ? cls.getGrade() : "");
        report.put("basicInfo", info);

        // 积分变化（近30天每日累计）
        List<Map<String, Object>> creditTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long dayCredits = creditMapper.selectList(
                new LambdaQueryWrapper<CreditTransaction>()
                    .eq(CreditTransaction::getStudentId, s.getId())
                    .eq(CreditTransaction::getTransactionType, "earn")
                    .le(CreditTransaction::getCreateTime, day.atTime(23, 59, 59)))
                .stream().mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
            creditTrend.add(Map.of("date", day.toString(), "credits", dayCredits));
        }
        report.put("creditTrend", creditTrend);

        // 任务统计
        List<com.school.teaching.entity.TaskSubmission> allSubs = List.of();
        if (cls != null) {
            List<com.school.teaching.entity.Task> classTasks = taskMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.Task>()
                    .eq(com.school.teaching.entity.Task::getTargetType, "CLASS")
                    .eq(com.school.teaching.entity.Task::getTargetId, cls.getId()));
            Set<Long> taskIds = classTasks.stream().map(com.school.teaching.entity.Task::getId).collect(Collectors.toSet());
            if (!taskIds.isEmpty()) {
                allSubs = submissionMapper.selectList(
                    new LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                        .in(com.school.teaching.entity.TaskSubmission::getTaskId, taskIds)
                        .eq(com.school.teaching.entity.TaskSubmission::getStudentId, s.getId()));
            }
        }
        long submitted = allSubs.stream().filter(sub -> !"PENDING".equals(sub.getStatus())).count();
        long graded = allSubs.stream().filter(sub -> "GRADED".equals(sub.getStatus()) && sub.getScore() != null).count();
        List<BigDecimal> scoreList = allSubs.stream()
            .filter(sub -> sub.getScore() != null)
            .map(TaskSubmission::getScore)
            .collect(Collectors.toList());
        double avgScore = graded > 0 ? ScoreUtils.avg(scoreList) : 0;
        Map<String, Object> taskStats = new LinkedHashMap<>();
        taskStats.put("totalTasks", allSubs.size());
        taskStats.put("submitted", submitted);
        taskStats.put("graded", graded);
        taskStats.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        report.put("taskStats", taskStats);

        // 班主任寄语
        String sem = currentSemester();
        StudentRemark remark = remarkMapper.selectOne(
            new LambdaQueryWrapper<StudentRemark>()
                .eq(StudentRemark::getStudentId, s.getId()).eq(StudentRemark::getSemester, sem));
        report.put("teacherRemark", remark != null ? remark.getRemark() : "");

        // 成长足迹（最近20条）
        List<StudentTimeline> timeline = timelineService.getByStudentId(s.getId());
        report.put("timeline", timeline.size() > 20 ? timeline.subList(0, 20) : timeline);

        return report;
    }
}
