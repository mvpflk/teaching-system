package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.PrecisionProfile;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.Student;
import com.school.teaching.mapper.PrecisionProfileMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionEffectService {

    @Autowired private PrecisionProfileMapper profileMapper;
    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private StudentMapper studentMapper;

    private static final int MAX_PROFILES = 500;
    private static final int MAX_PROGRESS = 2000;

    /**
     * 获取提分效果仪表盘数据
     * @param classId 班级ID（可选，null表示全部）
     * @param subject 学科（可选，null表示全部）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEffectDashboard(Long classId, String subject) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 预加载班级学生ID（classId 有值时只查一次）
        Set<Long> classStudentIds = classId != null ? loadClassStudentIds(classId) : null;

        // 1. 总体提分率
        result.put("overall", calculateOverallEffect(classStudentIds, subject));

        // 2. 学生提分详情
        List<Map<String, Object>> studentEffects = calculateStudentEffects(classStudentIds, subject);
        result.put("studentEffects", studentEffects);

        // 3. 班级平均提分
        double avgImprovement = studentEffects.stream()
            .filter(s -> s.get("improvement") instanceof Number)
            .mapToDouble(s -> ((Number) s.get("improvement")).doubleValue())
            .average()
            .orElse(0);
        result.put("avgImprovement", Math.round(avgImprovement * 10) / 10.0);

        // 4. 提分学生占比
        long improvedCount = studentEffects.stream()
            .filter(s -> s.get("improvement") instanceof Number)
            .filter(s -> ((Number) s.get("improvement")).doubleValue() > 0)
            .count();
        result.put("improvedRate", studentEffects.isEmpty() ? 0 :
            Math.round((double) improvedCount / studentEffects.size() * 100));

        // 5. 知识点掌握度分布
        result.put("masteryChanges", calculateMasteryChanges(classStudentIds, subject));

        return result;
    }

    /** 批量加载班级学生ID，避免后续逐条查询 */
    private Set<Long> loadClassStudentIds(Long classId) {
        if (classId == null) return null;
        return studentMapper.selectList(
            new LambdaQueryWrapper<Student>()
                .select(Student::getId)
                .eq(Student::getClassId, classId)
                .last("LIMIT 200"))
            .stream().map(Student::getId).collect(Collectors.toSet());
    }

    private Map<String, Object> calculateOverallEffect(Set<Long> classStudentIds, String subject) {
        Map<String, Object> effect = new LinkedHashMap<>();

        // 构建查询条件 — 过滤下推到 SQL
        LambdaQueryWrapper<PrecisionProfile> qw = new LambdaQueryWrapper<PrecisionProfile>()
            .isNotNull(PrecisionProfile::getDiagnoseScore)
            .isNotNull(PrecisionProfile::getLastOnlineTestScore)
            .gt(PrecisionProfile::getDiagnoseScore, 0)
            .gt(PrecisionProfile::getLastOnlineTestScore, 0);
        if (subject != null && !subject.isEmpty()) {
            qw.eq(PrecisionProfile::getSubject, subject);
        }
        qw.last("LIMIT " + MAX_PROFILES);

        List<PrecisionProfile> profiles = profileMapper.selectList(qw);

        // 班级过滤（内存过滤 — studentId 散列无法下推为高效 SQL）
        if (classStudentIds != null) {
            profiles = profiles.stream()
                .filter(p -> classStudentIds.contains(p.getStudentId()))
                .collect(Collectors.toList());
        }

        int totalStudents = profiles.size();
        int improvedStudents = 0;
        int totalImprovement = 0;

        for (PrecisionProfile p : profiles) {
            int improvement = p.getLastOnlineTestScore() - p.getDiagnoseScore();
            if (improvement > 0) {
                improvedStudents++;
                totalImprovement += improvement;
            }
        }

        effect.put("totalStudents", totalStudents);
        effect.put("improvedStudents", improvedStudents);
        effect.put("improvedRate", totalStudents > 0 ?
            Math.round((double) improvedStudents / totalStudents * 100) : 0);
        effect.put("avgImprovement", improvedStudents > 0 ?
            Math.round((double) totalImprovement / improvedStudents * 10) / 10.0 : 0);

        return effect;
    }

    private List<Map<String, Object>> calculateStudentEffects(Set<Long> classStudentIds, String subject) {
        LambdaQueryWrapper<PrecisionProfile> qw = new LambdaQueryWrapper<PrecisionProfile>()
            .isNotNull(PrecisionProfile::getDiagnoseScore)
            .isNotNull(PrecisionProfile::getLastOnlineTestScore)
            .gt(PrecisionProfile::getDiagnoseScore, 0)
            .gt(PrecisionProfile::getLastOnlineTestScore, 0);
        if (subject != null && !subject.isEmpty()) {
            qw.eq(PrecisionProfile::getSubject, subject);
        }
        qw.last("LIMIT " + MAX_PROFILES);

        List<PrecisionProfile> profiles = profileMapper.selectList(qw);

        // 班级过滤
        if (classStudentIds != null) {
            profiles = profiles.stream()
                .filter(p -> classStudentIds.contains(p.getStudentId()))
                .collect(Collectors.toList());
        }

        // 每个学生取最佳学科成绩（uk_student_subject 保证每个学生每学科最多一条）
        Map<Long, PrecisionProfile> bestProfiles = new LinkedHashMap<>();
        for (PrecisionProfile p : profiles) {
            PrecisionProfile existing = bestProfiles.get(p.getStudentId());
            if (existing == null || p.getLastOnlineTestScore() > existing.getLastOnlineTestScore()) {
                bestProfiles.put(p.getStudentId(), p);
            }
        }

        List<Map<String, Object>> studentEffects = new ArrayList<>();
        for (PrecisionProfile p : bestProfiles.values()) {
            Map<String, Object> effect = new LinkedHashMap<>();
            effect.put("studentId", p.getStudentId());
            effect.put("subject", p.getSubject());
            effect.put("diagnoseScore", p.getDiagnoseScore());
            effect.put("testScore", p.getLastOnlineTestScore());
            effect.put("improvement", p.getLastOnlineTestScore() - p.getDiagnoseScore());
            effect.put("streakWeeks", p.getStreakWeeks());
            studentEffects.add(effect);
        }

        studentEffects.sort((a, b) -> Integer.compare(
            ((Number) b.getOrDefault("improvement", 0)).intValue(),
            ((Number) a.getOrDefault("improvement", 0)).intValue()));

        return studentEffects;
    }

    private Map<String, Object> calculateMasteryChanges(Set<Long> classStudentIds, String subject) {
        Map<String, Object> changes = new LinkedHashMap<>();

        LambdaQueryWrapper<PrecisionProgress> qw = new LambdaQueryWrapper<PrecisionProgress>();
        if (subject != null && !subject.isEmpty()) {
            qw.eq(PrecisionProgress::getSubject, subject);
        }
        qw.last("LIMIT " + MAX_PROGRESS);

        List<PrecisionProgress> allProgress = progressMapper.selectList(qw);

        // 班级过滤 — 仅当指定班级时过滤
        if (classStudentIds != null) {
            allProgress = allProgress.stream()
                .filter(p -> classStudentIds.contains(p.getStudentId()))
                .collect(Collectors.toList());
        }

        long mastered = 0, learning = 0, weak = 0;
        for (PrecisionProgress p : allProgress) {
            if (p.getMasteryPercent() == null) continue;
            int m = p.getMasteryPercent().intValue();
            if (m >= 80) mastered++;
            else if (m >= 40) learning++;
            else weak++;
        }

        long total = mastered + learning + weak;
        changes.put("mastered", mastered);
        changes.put("learning", learning);
        changes.put("weak", weak);
        changes.put("total", total);
        changes.put("masteredRate", total > 0 ? Math.round((double) mastered / total * 100) : 0);

        return changes;
    }
}
