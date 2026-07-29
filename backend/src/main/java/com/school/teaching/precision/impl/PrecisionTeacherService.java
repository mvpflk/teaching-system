package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.AiOutput;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.PrecisionVocabulary;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.StudentGroup;
import com.school.teaching.entity.StudentGroupMember;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskQuestion;
import com.school.teaching.entity.TeacherClass;
import com.school.teaching.entity.User;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.mapper.AiOutputMapper;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.PrecisionVocabularyMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.StudentGroupMapper;
import com.school.teaching.mapper.StudentGroupMemberMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.TaskMapper;
import com.school.teaching.mapper.TaskQuestionMapper;
import com.school.teaching.mapper.TeacherClassMapper;
import com.school.teaching.mapper.TeacherMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.SystemService;
import com.school.teaching.utils.ScoreUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionTeacherService {

    @Autowired
    private PrecisionProgressMapper progressMapper;

    @Autowired
    private PrecisionVocabularyMapper vocabMapper;

    @Autowired
    private KnowledgeNodeMapper nodeMapper;

    @Autowired
    private QuestionBankMapper questionMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClassesMapper classesMapper;

    @Autowired
    private StudentGroupMapper groupMapper;

    @Autowired
    private StudentGroupMemberMapper groupMemberMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskQuestionMapper taskQuestionMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherClassMapper teacherClassMapper;

    @Autowired
    private WrongQuestionMapper wrongMapper;

    @Autowired
    private DictSubjectMapper subjectMapper;

    @Autowired(required = false)
    private AiOutputMapper aiOutputMapper;

    @Autowired(required = false)
    private PrecisionMathService mathService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private PrecisionHelper helper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Set<Long> getTeacherClassIds(Long teacherUserId) {
        return getTeacherClassIds(teacherUserId, null);
    }

    private Set<Long> getTeacherClassIds(Long teacherUserId, String subject) {
        if (teacherUserId == null) return Set.of();
        Set<Long> classIds = new HashSet<>();
        boolean hasSubject = subject != null && !subject.isEmpty();

        com.school.teaching.entity.Teacher t = teacherMapper.selectOne(
            new LambdaQueryWrapper<com.school.teaching.entity.Teacher>()
                .eq(com.school.teaching.entity.Teacher::getUserId, teacherUserId));
        Set<Long> teachingClassIds = new HashSet<>();
        if (t != null) {
            LambdaQueryWrapper<TeacherClass> tcQw = new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, t.getId());
            if (hasSubject) {
                tcQw.eq(TeacherClass::getSubject, subject);
            }
            List<TeacherClass> tcList = teacherClassMapper.selectList(tcQw);

            if (!hasSubject) {
                Set<String> precisionSubjects = getPrecisionSubjects();
                tcList = tcList.stream()
                    .filter(tc -> tc.getSubject() != null && precisionSubjects.contains(tc.getSubject()))
                    .collect(Collectors.toList());
            }
            tcList.forEach(tc -> {
                classIds.add(tc.getClassId());
                teachingClassIds.add(tc.getClassId());
            });
        }

        List<Classes> headClasses = classesMapper.selectList(
            new LambdaQueryWrapper<Classes>().eq(Classes::getHeadTeacherId, teacherUserId));
        for (Classes c : headClasses) {
            if (hasSubject) {
                if (teachingClassIds.contains(c.getId())) {
                    classIds.add(c.getId());
                }
            } else {
                classIds.add(c.getId());
            }
        }

        return classIds;
    }

    private Set<String> getPrecisionSubjects() {
        try {
            List<com.school.teaching.entity.DictSubject> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.DictSubject>()
                    .like(com.school.teaching.entity.DictSubject::getSubjectName, "[职高]")
                    .eq(com.school.teaching.entity.DictSubject::getStatus, 1));
            return subjects.stream()
                .map(com.school.teaching.entity.DictSubject::getSubjectName)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of("数学[职高]", "英语[职高]", "信息技术应用基础[职高]");
        }
    }

    private List<Map<String, Object>> buildClassTrend(List<Student> classStudents) {
        Set<Long> studentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) return List.of();

        List<PrecisionProgress> progs = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds));
        if (progs.isEmpty()) {
            return buildFallbackTrend(classStudents);
        }

        Map<String, List<Double>> weekScores = new LinkedHashMap<>();
        for (PrecisionProgress p : progs) {
            if (p.getUpdateTime() == null || p.getMasteryPercent() == null) continue;
            String weekKey = p.getUpdateTime().toLocalDate().toString();
            weekScores.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(p.getMasteryPercent().doubleValue());
        }

        List<Map<String, Object>> result = weekScores.entrySet().stream()
            .sorted(Map.Entry.<String, List<Double>>comparingByKey().reversed())
            .limit(5)
            .map(e -> {
                double avg = ScoreUtils.avgDouble(e.getValue());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", e.getKey());
                m.put("averageScore", Math.round(avg));
                return m;
            })
            .collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    private List<Map<String, Object>> buildFallbackTrend(List<Student> classStudents) {
        List<Map<String, Object>> scores = new ArrayList<>();
        for (Student s : classStudents) {
            if (s.getPrecisionProfile() == null) continue;
            try {
                Map<String, Object> profile = objectMapper.readValue(s.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                for (var entry : profile.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subjProfile = (Map<String, Object>) entry.getValue();
                    if (subjProfile != null && subjProfile.get("diagnoseScore") instanceof Number n
                        && subjProfile.get("lastDiagnoseAt") != null) {
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("date", subjProfile.get("lastDiagnoseAt").toString());
                        point.put("averageScore", n.intValue());
                        scores.add(point);
                    }
                }
            } catch (Exception e) {
                log.debug("解析学生 profile 失败 sid={}: {}", s.getId(), e.getMessage());
            }
        }
        if (scores.isEmpty()) return List.of();

        Map<String, List<Integer>> dateGroups = new LinkedHashMap<>();
        for (Map<String, Object> s : scores) {
            String date = (String) s.get("date");
            int score = ((Number) s.get("averageScore")).intValue();
            dateGroups.computeIfAbsent(date, k -> new ArrayList<>()).add(score);
        }
        return dateGroups.entrySet().stream()
            .sorted(Map.Entry.<String, List<Integer>>comparingByKey().reversed())
            .limit(5)
            .map(e -> {
                double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", e.getKey());
                m.put("averageScore", Math.round(avg));
                return m;
            })
            .sorted((a, b) -> String.valueOf(a.get("date")).compareTo(String.valueOf(b.get("date"))))
            .collect(Collectors.toList());
    }

    public Map<String, Object> teacherOverview(Long teacherUserId) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return Map.of("studentCount", 0);

        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) return Map.of("studentCount", 0);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<PrecisionProgress> activeMathList = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .select(PrecisionProgress::getStudentId)
                .in(PrecisionProgress::getStudentId, studentIds)
                .eq(PrecisionProgress::getSubject, "数学[职高]")
                .ge(PrecisionProgress::getLastPracticeAt, weekAgo)
                .groupBy(PrecisionProgress::getStudentId));
        long activeMath = activeMathList.size();
        List<PrecisionVocabulary> activeEngList = vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .select(PrecisionVocabulary::getStudentId)
                .in(PrecisionVocabulary::getStudentId, studentIds)
                .ge(PrecisionVocabulary::getLastReviewAt, weekAgo)
                .groupBy(PrecisionVocabulary::getStudentId));
        Set<Long> activeEngIds = new HashSet<>();
        activeEngList.forEach(v -> activeEngIds.add(v.getStudentId()));
        List<PrecisionProgress> activeEngProgress = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .select(PrecisionProgress::getStudentId)
                .in(PrecisionProgress::getStudentId, studentIds)
                .eq(PrecisionProgress::getSubject, "英语[职高]")
                .ge(PrecisionProgress::getLastPracticeAt, weekAgo)
                .groupBy(PrecisionProgress::getStudentId));
        activeEngProgress.forEach(p -> activeEngIds.add(p.getStudentId()));
        long activeEng = activeEngIds.size();
        List<PrecisionProgress> weakProgs = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds)
                .lt(PrecisionProgress::getMasteryPercent, 60));
        Set<Long> unmasteredIds = weakProgs.stream()
            .map(PrecisionProgress::getStudentId).collect(Collectors.toSet());
        List<PrecisionVocabulary> weakVocabStudents = vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .select(PrecisionVocabulary::getStudentId)
                .in(PrecisionVocabulary::getStudentId, studentIds)
                .lt(PrecisionVocabulary::getMasterLevel, 3)
                .groupBy(PrecisionVocabulary::getStudentId));
        weakVocabStudents.forEach(v -> unmasteredIds.add(v.getStudentId()));
        long unmasteredCount = unmasteredIds.size();

        return Map.of("studentCount", studentIds.size(),
            "mathActive", (int) activeMath, "englishActive", (int) activeEng,
            "unmasteredCount", (int) unmasteredCount);
    }

    public List<Map<String, Object>> teacherStudents(Long teacherUserId, Long groupId, String subject) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId, null);
        if (classIds.isEmpty()) return List.of();
        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        Map<Long, Long> studentClassMap = students.stream()
            .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));
        Map<Long, String> classNameMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            List<Classes> classList = classesMapper.selectBatchIds(classIds);
            classList.forEach(c -> classNameMap.put(c.getId(), c.getClassName() != null ? c.getClassName() : "未命名"));
        }
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, String> userRealNameMap = userIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : "未知"));
        Map<Long, String> nameMap = new HashMap<>();
        for (Student s : students) {
            nameMap.put(s.getId(), userRealNameMap.getOrDefault(s.getUserId(), "未知"));
        }
        Map<Long, String> profileMap = new HashMap<>();
        for (Student s : students) {
            if (s.getPrecisionProfile() != null) profileMap.put(s.getId(), s.getPrecisionProfile());
        }
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<PrecisionProgress> allProgress = studentIds.isEmpty() ? List.of() : progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>().in(PrecisionProgress::getStudentId, studentIds));
        Map<Long, List<PrecisionProgress>> progByStudent = new HashMap<>();
        for (PrecisionProgress p : allProgress) {
            progByStudent.computeIfAbsent(p.getStudentId(), k -> new ArrayList<>()).add(p);
        }
        Map<Long, LocalDateTime> lastActiveMap = new HashMap<>();
        Map<Long, Integer> weeklyCountMap = new HashMap<>();
        for (PrecisionProgress p : allProgress) {
            if (p.getLastPracticeAt() != null) {
                LocalDateTime existing = lastActiveMap.get(p.getStudentId());
                if (existing == null || p.getLastPracticeAt().isAfter(existing)) {
                    lastActiveMap.put(p.getStudentId(), p.getLastPracticeAt());
                }
            }
            if (p.getLastPracticeAt() != null && p.getLastPracticeAt().isAfter(weekAgo)) {
                weeklyCountMap.merge(p.getStudentId(), 1, Integer::sum);
            }
        }
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);
        Map<Long, Integer> prevWeekCountMap = new HashMap<>();
        for (PrecisionProgress p : allProgress) {
            if (p.getLastPracticeAt() != null
                && p.getLastPracticeAt().isAfter(twoWeeksAgo)
                && !p.getLastPracticeAt().isAfter(weekAgo)) {
                prevWeekCountMap.merge(p.getStudentId(), 1, Integer::sum);
            }
        }

        Map<Long, Boolean> hasMathAct = new HashMap<>();
        Map<Long, Boolean> hasEngAct = new HashMap<>();
        Map<Long, Integer> vocabCountMap = new HashMap<>();
        for (PrecisionProgress p : allProgress) {
            if ("数学[职高]".equals(p.getSubject())) hasMathAct.put(p.getStudentId(), true);
            if ("英语[职高]".equals(p.getSubject())) hasEngAct.put(p.getStudentId(), true);
        }
        if (!studentIds.isEmpty()) {
            List<PrecisionVocabulary> allVocab = vocabMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabulary>()
                    .in(PrecisionVocabulary::getStudentId, studentIds));
            for (PrecisionVocabulary v : allVocab) {
                hasEngAct.put(v.getStudentId(), true);
                vocabCountMap.merge(v.getStudentId(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long sid : studentIds) {
            String profileJson = profileMap.get(sid);
            int mathEstimate = 0, engVocab = 0;
            int streakWeeks = 0, lastTestScore = 0;
            boolean warning = false, active = false;
            LocalDateTime lastActive = lastActiveMap.get(sid);
            int weeklyCount = weeklyCountMap.getOrDefault(sid, 0);
            int prevWeekCount = prevWeekCountMap.getOrDefault(sid, 0);
            boolean recentlyActive = lastActive != null && lastActive.isAfter(weekAgo);

            if (profileJson != null) {
                try {
                    Map<String, Object> profile = objectMapper.readValue(profileJson, new TypeReference<Map<String, Object>>() {});
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mathProf = (Map<String, Object>) profile.get("数学[职高]");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> engProf = (Map<String, Object>) profile.get("英语[职高]");
                    if (engProf == null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> engAlt = (Map<String, Object>) profile.get("english");
                        if (engAlt != null) engProf = engAlt;
                    }

                    if (mathProf != null) {
                        mathEstimate = mathProf.get("estimatedScore") instanceof Number n ? n.intValue() : 0;
                    }
                    if (engProf != null) {
                        Object vs = engProf.get("vocabSize");
                        if (vs instanceof Number) {
                            engVocab = ((Number) vs).intValue();
                        } else {
                            Object vk = engProf.get("vocabKnown");
                            if (vk instanceof Number) engVocab = ((Number) vk).intValue();
                        }
                    }

                    if (subject != null && !subject.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subProf = (Map<String, Object>) profile.get(subject);
                        boolean engFallback = false;
                        if (subProf == null && "英语[职高]".equals(subject) && engProf != null) {
                            subProf = new LinkedHashMap<>(engProf);
                            engFallback = true;
                        }
                        if (subProf != null) {
                            if (engFallback) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> subjProf = (Map<String, Object>) profile.get(subject);
                                if (subjProf != null) {
                                    if (subjProf.get("streakWeeks") instanceof Number n) subProf.put("streakWeeks", n.intValue());
                                    if (subjProf.get("lastOnlineTestScore") instanceof Number n) subProf.put("lastOnlineTestScore", n.intValue());
                                    if (subjProf.get("diagnoseScore") instanceof Number n) subProf.put("diagnoseScore", n.intValue());
                                }
                                if (!subProf.containsKey("streakWeeks") && subProf.get("streak") instanceof Number n) {
                                    subProf.put("streakWeeks", n.intValue());
                                }
                            }
                            streakWeeks = PrecisionHelper.toInt(subProf.get("streakWeeks"), 0);
                            lastTestScore = PrecisionHelper.toInt(subProf.get("lastOnlineTestScore"), 0);
                            int diagScore = PrecisionHelper.toInt(subProf.get("diagnoseScore"), 0);
                            if ("英语[职高]".equals(subject)) {
                                if (streakWeeks > 0 || lastTestScore > 0 || hasEngAct.containsKey(sid)) {
                                    active = true;
                                    if (diagScore > 0 && streakWeeks < 2) warning = true;
                                }
                            } else {
                                active = true;
                                if (diagScore > 0 && streakWeeks < 2) warning = true;
                            }
                        }
                    } else {
                        if (mathProf != null) {
                            streakWeeks = Math.max(streakWeeks, mathProf.get("streakWeeks") instanceof Number n ? n.intValue() : 0);
                            lastTestScore = Math.max(lastTestScore, mathProf.get("lastOnlineTestScore") instanceof Number n ? n.intValue() : 0);
                        }
                        if (engProf != null) {
                            int engStreak = PrecisionHelper.toInt(engProf.get("streakWeeks"), 0);
                            if (engStreak == 0) engStreak = PrecisionHelper.toInt(engProf.get("streak"), 0);
                            streakWeeks = Math.max(streakWeeks, engStreak);
                            lastTestScore = Math.max(lastTestScore, PrecisionHelper.toInt(engProf.get("lastOnlineTestScore"), 0));
                        }
                        int diagScore = (mathProf != null && mathProf.get("diagnoseScore") instanceof Number n ? n.intValue() : 0);
                        int engDiag = (engProf != null && engProf.get("diagnoseScore") instanceof Number n ? n.intValue() : 0);
                        if ((diagScore > 0 && streakWeeks < 2) || (engDiag > 0 && streakWeeks < 2)) warning = true;
                        if (mathProf != null || engProf != null) active = true;
                        if (!active && (hasMathAct.containsKey(sid) || hasEngAct.containsKey(sid))) active = true;
                    }
                } catch (Exception e) {
                    log.warn("解析偏科画像失败 sid={}", sid, e);
                }
            }

            String subj = subject;
            if (subj != null && !subj.isEmpty()) {
                if (!active) {
                    boolean hasAny = "数学[职高]".equals(subj)
                        ? hasMathAct.containsKey(sid)
                        : "英语[职高]".equals(subj)
                            ? hasEngAct.containsKey(sid) : false;
                    if (!hasAny) continue;
                    active = true;
                }
                if ("英语[职高]".equals(subj) && engVocab == 0) {
                    engVocab = vocabCountMap.getOrDefault(sid, 0);
                }
            }

            boolean isActive = recentlyActive || active;

            if (engVocab == 0) {
                engVocab = vocabCountMap.getOrDefault(sid, 0);
            }

            Long classId = studentClassMap.get(sid);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", sid);
            row.put("studentName", nameMap.getOrDefault(sid, "未知"));
            row.put("classId", classId);
            row.put("className", classId != null ? classNameMap.getOrDefault(classId, "") : "");
            row.put("mathEstimate", mathEstimate);
            row.put("engVocab", engVocab);
            row.put("streakWeeks", streakWeeks);
            row.put("lastTestScore", lastTestScore);
            row.put("warning", warning);
            row.put("active", isActive);
            row.put("lastActiveAt", lastActive != null ? lastActive.toString() : null);
            row.put("weeklyPracticeCount", weeklyCount);
            row.put("prevWeekPracticeCount", prevWeekCount);
            result.add(row);
        }
        result.sort((a, b) -> {
            boolean aActive = Boolean.TRUE.equals(a.get("active"));
            boolean bActive = Boolean.TRUE.equals(b.get("active"));
            if (aActive != bActive) return bActive ? 1 : -1;
            String aTime = (String) a.get("lastActiveAt");
            String bTime = (String) b.get("lastActiveAt");
            if (aTime != null && bTime != null) return bTime.compareTo(aTime);
            if (aTime != null) return -1;
            if (bTime != null) return 1;
            return Integer.compare((Integer) a.get("lastTestScore"), (Integer) b.get("lastTestScore"));
        });
        return result;
    }

    public int remindAll(Long teacherUserId, String subject) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId, subject);
        int count = 0;
        for (Long cid : classIds) {
            try {
                notificationService.notifyClassStudents(cid, "remedial_reminder",
                    "偏科提分提醒", "请于本周末完成" + subject + "线上小测并提交", null);
                count++;
            } catch (Exception e) {
                log.debug("提醒班级 {} 失败: {}", cid, e.getMessage());
            }
        }
        return count;
    }

    @Transactional
    public Map<String, Object> composeRemedialTask(Long teacherUserId, Long groupId, Long classId, String subject) {
        Set<Long> weakNodeIds = new HashSet<>();
        Set<Long> targetStudentIds = new HashSet<>();
        String targetName = "偏科组";

        if (groupId != null) {
            List<StudentGroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, groupId));
            if (!members.isEmpty()) {
                targetStudentIds = members.stream().map(StudentGroupMember::getStudentId).collect(Collectors.toSet());
            }
            StudentGroup group = groupMapper.selectById(groupId);
            targetName = group != null ? group.getName() : "偏科组";
        } else if (classId != null) {
            List<Student> classStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            targetStudentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());
            Classes cls = classesMapper.selectById(classId);
            targetName = cls != null ? cls.getClassName() : "班级";
        } else {
            Set<Long> classIds = getTeacherClassIds(teacherUserId, subject);
            if (!classIds.isEmpty()) {
                List<Student> allStudents = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
                targetStudentIds = allStudents.stream().map(Student::getId).collect(Collectors.toSet());
                targetName = "全体";
            }
        }

        if (!targetStudentIds.isEmpty()) {
            List<PrecisionProgress> weakProgress = progressMapper.selectList(
                new LambdaQueryWrapper<PrecisionProgress>()
                    .in(PrecisionProgress::getStudentId, targetStudentIds)
                    .eq(PrecisionProgress::getSubject, subject)
                    .lt(PrecisionProgress::getMasteryPercent, 60)
                    .orderByAsc(PrecisionProgress::getMasteryPercent));
            weakNodeIds = weakProgress.stream().map(PrecisionProgress::getNodeId).collect(Collectors.toSet());
        }

        List<QuestionBank> questions;
        if (weakNodeIds.isEmpty()) {
            questions = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getSubject, subject)
                    .last("ORDER BY RAND() LIMIT 15"));
        } else {
            questions = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getSubject, subject)
                    .in(QuestionBank::getCategoryId, weakNodeIds)
                    .last("ORDER BY RAND() LIMIT 15"));
            if (questions.size() < 10) {
                Set<Long> existingIds = questions.stream().map(QuestionBank::getId).collect(Collectors.toSet());
                List<QuestionBank> extra = questionMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getSubject, subject)
                        .notIn(!existingIds.isEmpty(), QuestionBank::getId, existingIds));
                Collections.shuffle(extra);
                for (QuestionBank q : extra) {
                    if (questions.size() >= 15) break;
                    if (!existingIds.contains(q.getId())) {
                        questions.add(q);
                        existingIds.add(q.getId());
                    }
                }
            }
        }
        if (questions.isEmpty()) {
            return Map.of("message", "题库暂无题目，请先导入种子数据，或联系管理员配置AI出题", "groupId", groupId);
        }

        if (questions.size() < 10) {
            try {
                boolean aiEnabled = systemService.getBooleanConfig("feature.ai_question_enabled", true);
                int dailyLimit = systemService.getIntConfig("ai.remedial_daily_limit", 20);
                if (aiEnabled && dailyLimit > 0) {
                    log.info("补强卷题目不足(仅{}题)，可考虑使用AI出题端点 /api/ai/questions/remedial 补充", questions.size());
                }
            } catch (Exception ignored) { log.warn("检查AI出题配置失败: {}", ignored.getMessage()); }
        }

        Long teacherIdObj = null;
        com.school.teaching.entity.Teacher t = teacherMapper.selectOne(
            new LambdaQueryWrapper<com.school.teaching.entity.Teacher>()
                .eq(com.school.teaching.entity.Teacher::getUserId, teacherUserId));
        if (t != null) teacherIdObj = t.getId();

        Task task = new Task();
        task.setTitle("补强卷·" + targetName + "·" + subject);
        task.setTaskType("FORMATIVE");
        task.setScoreType("POINT_100");
        task.setSubject(subject);
        task.setTotalScore(new java.math.BigDecimal(questions.size() * 2));
        if (classId != null && groupId == null) {
            task.setTargetType("CLASS");
            task.setTargetId(classId);
        } else {
            task.setTargetType("GROUP");
            task.setTargetId(groupId);
        }
        task.setTeacherId(teacherIdObj);
        task.setIsRequired(0);
        task.setAutoWrongbook(1);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setStatus("DRAFT");
        taskMapper.insert(task);

        int sort = 0;
        for (QuestionBank qb : questions) {
            TaskQuestion tq = new TaskQuestion();
            tq.setTaskId(task.getId());
            tq.setQuestionId(qb.getId());
            tq.setSortOrder(sort++);
            tq.setScore(java.math.BigDecimal.valueOf(2));
            tq.setSchoolId(1L);
            tq.setStageId(4L);
            taskQuestionMapper.insert(tq);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId());
        result.put("questionCount", questions.size());
        result.put("message", "已为「" + targetName + "」生成 " + questions.size() + " 道补强题");
        return result;
    }

    public List<Map<String, Object>> teacherWeakTop(Long teacherUserId, String subject, int topN) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId, null);
        if (classIds.isEmpty()) return List.of();

        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) return List.of();

        List<PrecisionProgress> weakList = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds)
                .eq(PrecisionProgress::getSubject, subject)
                .lt(PrecisionProgress::getMasteryPercent, 60));

        Map<Long, Long> nodeCounts = new LinkedHashMap<>();
        for (PrecisionProgress p : weakList) {
            nodeCounts.merge(p.getNodeId(), 1L, Long::sum);
        }

        // 英语词汇弱点查询——缓存复用，避免两次相同DB查询
        List<PrecisionVocabulary> weakVocabCache = null;
        if ("英语[职高]".equals(subject)) {
            weakVocabCache = vocabMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabulary>()
                    .in(PrecisionVocabulary::getStudentId, studentIds)
                    .lt(PrecisionVocabulary::getMasterLevel, 3));
            if (!weakVocabCache.isEmpty()) {
                Map<String, Long> wordCounts = weakVocabCache.stream()
                    .collect(Collectors.groupingBy(
                        v -> PrecisionHelper.fixEncoding(v.getWord()),
                        Collectors.collectingAndThen(
                            Collectors.mapping(PrecisionVocabulary::getStudentId, Collectors.toSet()),
                            s -> (long) s.size())));
                wordCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(topN)
                    .forEach(e -> nodeCounts.put(-Math.abs(e.getKey().hashCode() % 100000L), e.getValue()));
            }
        }

        if (nodeCounts.isEmpty()) return List.of();

        Map<Long, String> nodeNames = new HashMap<>();
        Set<Long> realNodeIds = nodeCounts.keySet().stream().filter(id -> id > 0).collect(Collectors.toSet());
        if (!realNodeIds.isEmpty()) {
            List<KnowledgeNode> nodes = nodeMapper.selectBatchIds(realNodeIds);
            nodes.forEach(n -> nodeNames.put(n.getId(), n.getName() != null ? n.getName() : "未知"));
        }
        if (weakVocabCache != null && !weakVocabCache.isEmpty()) {
            Map<Long, String> vocabWordMap = new HashMap<>();
            weakVocabCache.forEach(v -> {
                String word = PrecisionHelper.fixEncoding(v.getWord());
                long fakeId = -Math.abs(word.hashCode() % 100000L);
                if (nodeCounts.containsKey(fakeId)) vocabWordMap.putIfAbsent(fakeId, "🔤 " + word);
            });
            nodeNames.putAll(vocabWordMap);
        }

        return nodeCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(topN)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nodeId", e.getKey());
                m.put("name", nodeNames.getOrDefault(e.getKey(), "未命名"));
                m.put("errorCount", e.getValue().intValue());
                return m;
            }).collect(Collectors.toList());
    }

    public boolean remindStudent(Long teacherUserId, Long studentId, String subject) {
        Student st = studentMapper.selectById(studentId);
        if (st == null || st.getUserId() == null) return false;
        try {
            notificationService.notify(st.getUserId(), "remedial_reminder",
                "偏科提分提醒", "老师提醒你于本周末完成" + subject + "线上小测并提交", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getClassWeaknesses(Long classId) {
        List<Student> classStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        if (classStudents.isEmpty()) {
            return Map.of("weakNodes", List.of(), "diagnosisTrend", List.of());
        }
        Set<Long> studentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());

        Map<Long, Long> nodeErrorCount = new LinkedHashMap<>();

        List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .in(WrongQuestion::getStudentId, studentIds)
                .eq(WrongQuestion::getIsMastered, 0));
        if (!wqs.isEmpty()) {
            Set<Long> qIds = wqs.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
            Map<Long, Long> qErrorCount = wqs.stream()
                .collect(Collectors.groupingBy(WrongQuestion::getQuestionId, Collectors.counting()));
            List<QuestionBank> qbs = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>().in(QuestionBank::getId, qIds));
            for (QuestionBank qb : qbs) {
                Long cid = qb.getCategoryId();
                if (cid != null) {
                    nodeErrorCount.merge(cid, qErrorCount.getOrDefault(qb.getId(), 0L), Long::sum);
                }
            }
        }

        Map<String, Long> vocabWeakCount = new LinkedHashMap<>();
        List<PrecisionVocabulary> weakVocabs = vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .in(PrecisionVocabulary::getStudentId, studentIds)
                .lt(PrecisionVocabulary::getMasterLevel, 3));
        if (!weakVocabs.isEmpty()) {
            Map<String, Long> wordStudentCount = weakVocabs.stream()
                .collect(Collectors.groupingBy(
                    v -> PrecisionHelper.fixEncoding(v.getWord()),
                    Collectors.collectingAndThen(
                        Collectors.mapping(PrecisionVocabulary::getStudentId, Collectors.toSet()),
                        s -> (long) s.size())));
            vocabWeakCount.putAll(wordStudentCount);
        }

        List<PrecisionProgress> weakProgs = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds)
                .lt(PrecisionProgress::getMasteryPercent, 60));
        Map<Long, Long> progNodeCount = new LinkedHashMap<>();
        for (PrecisionProgress pp : weakProgs) {
            if (pp.getNodeId() != null) progNodeCount.merge(pp.getNodeId(), 1L, Long::sum);
        }
        progNodeCount.forEach((nid, cnt) -> nodeErrorCount.merge(nid, cnt, Math::max));

        boolean hasVocabWeak = !vocabWeakCount.isEmpty();
        boolean hasWeakData = !nodeErrorCount.isEmpty() || hasVocabWeak;

        if (!hasWeakData) {
            return Map.of("weakNodes", List.of(), "diagnosisTrend", buildClassTrend(classStudents));
        }

        Map<Long, String> nodeNameMap = new HashMap<>();
        if (!nodeErrorCount.isEmpty()) {
            List<KnowledgeNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>().in(KnowledgeNode::getId, nodeErrorCount.keySet()));
            nodes.forEach(n -> nodeNameMap.put(n.getId(), n.getName() != null ? n.getName() : "未知"));
        }

        List<Map<String, Object>> weakNodes = nodeErrorCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", nodeNameMap.getOrDefault(e.getKey(), "未知节点"));
                m.put("errorCount", e.getValue());
                return m;
            }).collect(Collectors.toList());

        if (hasVocabWeak) {
            List<Map<String, Object>> vocabNodes = new ArrayList<>();
            vocabWeakCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", "🔤 " + e.getKey());
                    m.put("errorCount", e.getValue());
                    vocabNodes.add(m);
                });
            weakNodes.addAll(vocabNodes);
            weakNodes.sort((a, b) -> Long.compare(
                ((Number) b.get("errorCount")).longValue(),
                ((Number) a.get("errorCount")).longValue()));
            if (weakNodes.size() > 10) weakNodes = new ArrayList<>(weakNodes.subList(0, 10));
        }

        List<Map<String, Object>> diagnosisTrend = buildClassTrend(classStudents);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weakNodes", weakNodes);
        result.put("diagnosisTrend", diagnosisTrend);
        return result;
    }

    public void assertTeacherOwnsStudent(Long teacherUserId, Long studentId) {
        if (teacherUserId == null || studentId == null)
            throw new BusinessException(403, "无权访问该学生数据");
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty())
            throw new BusinessException(403, "无权访问该学生数据");
        Student st = studentMapper.selectById(studentId);
        if (st == null || !classIds.contains(st.getClassId()))
            throw new BusinessException(403, "无权访问该学生数据");
    }

    public void assertTeacherOwnsClass(Long teacherUserId, Long classId) {
        if (teacherUserId == null || classId == null)
            throw new BusinessException(403, "无权访问该班级数据");
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (!classIds.contains(classId))
            throw new BusinessException(403, "无权访问该班级数据");
    }

    /** 教师端：查询待审核的解答题列表 */
    public List<Map<String, Object>> getPendingCalcReviews(Long teacherUserId, Long classId, int status) {
        Set<Long> classStudentIds = null;
        if (classId != null) {
            assertTeacherOwnsClass(teacherUserId, classId);
            classStudentIds = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().select(Student::getId).eq(Student::getClassId, classId))
                .stream().map(Student::getId).collect(Collectors.toSet());
        }

        LambdaQueryWrapper<AiOutput> qw = new LambdaQueryWrapper<AiOutput>()
                .eq(AiOutput::getOutputType, "CALC_REVIEW")
                .orderByDesc(AiOutput::getCreatedAt)
                .last("LIMIT 100");

        if (status >= 0) {
            qw.eq(AiOutput::getStatus, status);
        }

        List<AiOutput> reviews = aiOutputMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();

        ObjectMapper om = new ObjectMapper();
        for (AiOutput r : reviews) {
            if (r.getContent() == null) continue;
            try {
                Map<String, Object> content = om.readValue(r.getContent(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                Long sid = content.get("studentId") instanceof Number n ? n.longValue() : null;
                if (classStudentIds != null && sid != null && !classStudentIds.contains(sid)) continue;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("reviewId", r.getId());
                item.put("studentId", sid);
                item.put("questionId", content.get("questionId"));
                item.put("questionText", content.get("questionText"));
                item.put("studentAnswer", content.get("studentAnswer"));
                item.put("correctAnswer", content.get("correctAnswer"));
                item.put("aiScore", content.get("aiScore"));
                item.put("aiConfidence", content.get("aiConfidence"));
                item.put("aiFeedback", content.get("feedback"));
                item.put("status", r.getStatus());
                item.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);

                // 加载学生信息
                if (sid != null) {
                    Student st = studentMapper.selectById(sid);
                    if (st != null && st.getUserId() != null) {
                        User u = userMapper.selectById(st.getUserId());
                        item.put("studentName", u != null ? u.getRealName() : "学生" + sid);
                    } else {
                        item.put("studentName", "学生" + sid);
                    }
                }
                result.add(item);
            } catch (Exception e) {
                log.warn("解析待审解答题内容失败 id={}", r.getId(), e);
            }
        }
        return result;
    }

    /** 教师端：对解答题进行评分 */
    public Map<String, Object> gradeCalcReview(Long reviewId, int teacherScore, String teacherComment) {
        AiOutput review = aiOutputMapper.selectById(reviewId);
        if (review == null) throw new BusinessException(404, "待审记录不存在");
        if (!"CALC_REVIEW".equals(review.getOutputType())) throw new BusinessException(400, "该记录不是解答题审核");

        // 标记已审
        review.setStatus(1); // 1=已审
        review.setFeedback(teacherComment);
        review.setRating(teacherScore);
        review.setTeacherId(SecurityUtils.getCurrentUserId());
        aiOutputMapper.updateById(review);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviewId", reviewId);
        result.put("teacherScore", teacherScore);
        result.put("status", "graded");
        return result;
    }
}
