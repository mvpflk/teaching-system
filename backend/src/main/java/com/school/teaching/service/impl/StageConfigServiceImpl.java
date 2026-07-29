package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.StageConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StageConfigServiceImpl implements StageConfigService {

    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final StudentStageChangeLogMapper stageChangeLogMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final UserMapper userMapper;
    private final StudentClassHistoryMapper classHistoryMapper;
    private final EduStageConfigMapper configMapper;

    @Override
    public int batchUpdateClassType(List<Integer> classIds, String classType) {
        for (Integer id : classIds) {
            Classes c = classesMapper.selectById(id.longValue());
            if (c != null) { c.setClassType(classType); classesMapper.updateById(c); }
        }
        return classIds.size();
    }

    @Override
    public Map<String, Object> pageStageChangeLogs(int page, int size) {
        var q = new LambdaQueryWrapper<StudentStageChangeLog>().orderByDesc(StudentStageChangeLog::getCreatedAt);
        var pg = new Page<StudentStageChangeLog>(page, size);
        var result = stageChangeLogMapper.selectPage(pg, q);

        Set<Long> sids = result.getRecords().stream().map(StudentStageChangeLog::getStudentId).collect(Collectors.toSet());
        Set<Long> oids = result.getRecords().stream().map(StudentStageChangeLog::getApprovedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> snameMap = sids.isEmpty() ? Map.of() :
            studentMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(Student::getId, Student::getStudentNumber));
        Map<Long, String> onameMap = oids.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(oids).stream().collect(Collectors.toMap(User::getId, User::getRealName));

        List<Map<String, Object>> records = new ArrayList<>();
        for (StudentStageChangeLog l : result.getRecords()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", l.getId()); r.put("studentId", l.getStudentId());
            r.put("studentName", snameMap.getOrDefault(l.getStudentId(), ""));
            r.put("fromStageId", l.getFromStageId()); r.put("toStageId", l.getToStageId());
            r.put("changeDate", l.getChangeDate()); r.put("reason", l.getReason());
            r.put("operatorName", onameMap.getOrDefault(l.getApprovedBy(), ""));
            records.add(r);
        }
        return Map.of("records", records, "total", result.getTotal());
    }

    @Override
    public List<Map<String, Object>> getTeacherCrossTypeStats() {
        List<Teacher> teachers = teacherMapper.selectList(null);
        if (teachers.isEmpty()) return List.of();

        // 批量加载所有教师的任教班级关系
        Set<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toSet());
        Map<Long, List<TeacherClass>> tcMap = teacherClassMapper.selectList(
            new LambdaQueryWrapper<TeacherClass>().in(TeacherClass::getTeacherId, teacherIds))
            .stream().collect(Collectors.groupingBy(TeacherClass::getTeacherId));

        // 批量加载所有相关班级
        Set<Long> allCids = tcMap.values().stream().flatMap(List::stream)
            .map(TeacherClass::getClassId).collect(Collectors.toSet());
        Map<Long, Classes> classMap = allCids.isEmpty() ? Map.of() :
            classesMapper.selectBatchIds(allCids).stream().collect(Collectors.toMap(Classes::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Teacher t : teachers) {
            List<TeacherClass> tcs = tcMap.getOrDefault(t.getId(), List.of());
            Set<Long> cids = tcs.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
            if (cids.isEmpty()) continue;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("teacherId", t.getId()); r.put("classCount", cids.size());
            Set<String> types = cids.stream()
                .map(cid -> { Classes c = classMap.get(cid); return c != null && c.getClassType() != null ? c.getClassType() : "VOCATIONAL"; })
                .collect(Collectors.toSet());
            r.put("hasCrossType", types.size() > 1);
            r.put("classTypes", types);
            result.add(r);
        }
        return result;
    }

    @Override
    public Map<String, Object> getStageStats() {
        List<Classes> allClasses = classesMapper.selectList(null);
        Map<String, Long> classByType = allClasses.stream()
            .collect(Collectors.groupingBy(c -> c.getClassType() != null ? c.getClassType() : "VOCATIONAL", Collectors.counting()));

        List<Student> allStudents = studentMapper.selectList(null);
        Set<Long> cids = allStudents.stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> ctypeMap = classesMapper.selectBatchIds(cids).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c.getClassType() != null ? c.getClassType() : "VOCATIONAL"));
        Map<String, Long> stuByType = allStudents.stream()
            .collect(Collectors.groupingBy(s -> ctypeMap.getOrDefault(s.getClassId(), "VOCATIONAL"), Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("classByType", classByType);
        stats.put("studentByType", stuByType);
        return stats;
    }

    @Override
    public List<Map<String, Object>> checkDataConsistency() {
        List<Map<String, Object>> mismatches = new ArrayList<>();
        List<Student> students = studentMapper.selectList(null);
        for (Student s : students) {
            if (s.getClassId() == null) continue;
            StudentClassHistory h = classHistoryMapper.selectOne(
                new LambdaQueryWrapper<StudentClassHistory>()
                    .eq(StudentClassHistory::getStudentId, s.getId()).isNull(StudentClassHistory::getEndDate));
            if (h == null) continue;
            Classes c = classesMapper.selectById(h.getClassId());
            if (c == null) continue;
            String stuType = s.getCurrentType() != null ? s.getCurrentType() : "VOCATIONAL";
            String clsType = c.getClassType() != null ? c.getClassType() : "VOCATIONAL";
            if (!stuType.equals(clsType)) {
                mismatches.add(Map.of("studentId", s.getId(), "studentNo", s.getStudentNumber(),
                    "studentType", stuType, "classType", clsType, "classId", c.getId(), "className", c.getClassName()));
            }
        }
        return mismatches;
    }

    @Override
    public List<EduStageConfig> listConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public void toggleConfig(Long id, Integer enabled) {
        var cfg = configMapper.selectById(id);
        if (cfg != null) {
            if (enabled != null) cfg.setEnabled(enabled);
            configMapper.updateById(cfg);
        }
    }
}
