package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.SystemService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrecisionTeacherServiceTest {

    @Mock private PrecisionProgressMapper progressMapper;
    @Mock private PrecisionVocabularyMapper vocabMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private QuestionBankMapper questionMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ClassesMapper classesMapper;
    @Mock private StudentGroupMapper groupMapper;
    @Mock private StudentGroupMemberMapper groupMemberMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private TaskQuestionMapper taskQuestionMapper;
    @Mock private TeacherMapper teacherMapper;
    @Mock private TeacherClassMapper teacherClassMapper;
    @Mock private WrongQuestionMapper wrongMapper;
    @Mock private DictSubjectMapper subjectMapper;
    @Mock private PrecisionMathService mathService;
    @Mock private NotificationService notificationService;
    @Mock private SystemService systemService;
    @Mock private PrecisionHelper helper;

    @InjectMocks
    private PrecisionTeacherService teacherService;

    private static final Long TEACHER_USER_ID = 100L;
    private static final Long TEACHER_ID = 1L;
    private static final Long GROUP_ID = 200L;
    private static final Long CLASS_ID = 300L;
    private static final String SUBJECT = "数学[职高]";

    // ═══════════════════════════════════════════
    //  Test data helpers
    // ═══════════════════════════════════════════

    private Student student(Long id, Long classId) {
        Student s = new Student();
        s.setId(id);
        s.setClassId(classId);
        return s;
    }

    private QuestionBank question(Long id, String subject, Long catId) {
        QuestionBank q = new QuestionBank();
        q.setId(id);
        q.setSubject(subject);
        q.setCategoryId(catId);
        q.setQuestionText("题" + id);
        q.setQuestionType("SINGLE_CHOICE");
        q.setCorrectAnswer("A");
        q.setStatus(1);
        return q;
    }

    private PrecisionProgress progress(Long studentId, Long nodeId, int mastery) {
        PrecisionProgress p = new PrecisionProgress();
        p.setStudentId(studentId);
        p.setNodeId(nodeId);
        p.setSubject(SUBJECT);
        p.setMasteryPercent(java.math.BigDecimal.valueOf(mastery));
        p.setStatus("learning");
        return p;
    }

    private void stubTeacherLookup() {
        com.school.teaching.entity.Teacher t = new com.school.teaching.entity.Teacher();
        t.setId(TEACHER_ID);
        t.setUserId(TEACHER_USER_ID);
        when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    }

    /** 返回一个可以 shuffle/add 的可变列表，模拟 DB 查询 */
    @SafeVarargs
    private static <T> List<T> mutableList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    // ═══════════════════════════════════════════
    //  composeRemedialTask tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("按班级生成: classId 有效 → 从该班学生找弱项知识点")
    void composeRemedialTask_byClass() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID), student(2L, CLASS_ID))));

        Classes cls = new Classes(); cls.setId(CLASS_ID); cls.setClassName("高三1班");
        when(classesMapper.selectById(CLASS_ID)).thenReturn(cls);

        List<PrecisionProgress> weak = mutableList(progress(1L, 10L, 30), progress(2L, 10L, 40), progress(1L, 20L, 50));
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(weak);

        List<QuestionBank> questions = mutableList(
            question(1L, SUBJECT, 10L), question(2L, SUBJECT, 20L), question(3L, SUBJECT, 10L));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(questions);

        stubTeacherLookup();
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, null, CLASS_ID, SUBJECT);

        assertTrue(result.containsKey("taskId"));
        assertEquals(3, result.get("questionCount"));
        assertTrue(((String) result.get("message")).contains("高三1班"));

        ArgumentCaptor<Task> taskCap = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(taskCap.capture());
        Task saved = taskCap.getValue();
        assertTrue(saved.getTitle().contains("高三1班"));
        assertEquals("CLASS", saved.getTargetType());
        assertEquals(CLASS_ID, saved.getTargetId());
        assertEquals(4L, saved.getStageId());
        assertEquals(1L, saved.getSchoolId());
        assertEquals(0, saved.getIsRequired().intValue());
        assertEquals(1, saved.getAutoWrongbook().intValue());
    }

    @Test
    @DisplayName("按分组生成: groupId 有效 → 从该组成员找弱项知识点")
    void composeRemedialTask_byGroup() {
        StudentGroupMember m1 = new StudentGroupMember();
        m1.setStudentId(1L); m1.setGroupId(GROUP_ID);
        StudentGroupMember m2 = new StudentGroupMember();
        m2.setStudentId(2L); m2.setGroupId(GROUP_ID);
        when(groupMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m1, m2));

        StudentGroup group = new StudentGroup(); group.setId(GROUP_ID); group.setName("偏科A组");
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);

        List<PrecisionProgress> weak = mutableList(progress(1L, 10L, 30));
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(weak);

        List<QuestionBank> questions = mutableList(question(1L, SUBJECT, 10L));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(questions);

        stubTeacherLookup();
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, GROUP_ID, null, SUBJECT);

        assertTrue(result.containsKey("taskId"));
        assertEquals(1, result.get("questionCount"));
        assertTrue(((String) result.get("message")).contains("偏科A组"));
    }

    @Test
    @DisplayName("教师全局生成: groupId/classId 都 null → 取教师所有班级学生")
    void composeRemedialTask_teacherWide() {
        // teacherMapper used in getTeacherClassIds AND for task.teacherId
        com.school.teaching.entity.Teacher t = new com.school.teaching.entity.Teacher();
        t.setId(TEACHER_ID); t.setUserId(TEACHER_USER_ID);
        when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);

        TeacherClass tc = new TeacherClass();
        tc.setTeacherId(TEACHER_ID); tc.setClassId(CLASS_ID); tc.setSubject(SUBJECT);
        when(teacherClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tc));

        when(classesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID), student(2L, CLASS_ID))));

        List<PrecisionProgress> weak = mutableList(progress(1L, 10L, 30), progress(2L, 20L, 50));
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(weak);

        List<QuestionBank> questions = mutableList(question(1L, SUBJECT, 10L), question(2L, SUBJECT, 20L));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(questions);

        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, null, null, SUBJECT);

        assertTrue(result.containsKey("taskId"));
        assertEquals(2, result.get("questionCount"));
        assertTrue(((String) result.get("message")).contains("全体"));
    }

    @Test
    @DisplayName("弱项知识点为空 → 随机选题")
    void composeRemedialTask_weakNodesEmpty_randomSelection() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID))));

        Classes cls = new Classes(); cls.setId(CLASS_ID); cls.setClassName("高三1班");
        when(classesMapper.selectById(CLASS_ID)).thenReturn(cls);

        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<QuestionBank> questions = new ArrayList<>();
        for (long i = 1; i <= 15; i++) questions.add(question(i, SUBJECT, i));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(questions);

        stubTeacherLookup();
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, null, CLASS_ID, SUBJECT);

        assertEquals(15, result.get("questionCount"));
    }

    @Test
    @DisplayName("弱项题目不足10道 → 补充随机题")
    void composeRemedialTask_weakQuestionsUnder10_supplement() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID))));

        Classes cls = new Classes(); cls.setId(CLASS_ID); cls.setClassName("高三1班");
        when(classesMapper.selectById(CLASS_ID)).thenReturn(cls);

        List<PrecisionProgress> weak = mutableList(progress(1L, 10L, 30));
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(weak);

        List<QuestionBank> weakQuestions = mutableList(
            question(1L, SUBJECT, 10L), question(2L, SUBJECT, 10L), question(3L, SUBJECT, 10L));
        // 第一次: 按弱项节点查询, 第二次: 补充随机
        when(questionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(weakQuestions)
            .thenReturn(new ArrayList<>(List.of(question(4L, SUBJECT, null))));

        stubTeacherLookup();
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, null, CLASS_ID, SUBJECT);

        assertEquals(4, result.get("questionCount"));
    }

    @Test
    @DisplayName("题库为空 → 返回提示信息")
    void composeRemedialTask_noQuestions_returnsMessage() {
        // 使用 group 路径（非 class 路径）避免 Map.of null value NPE
        StudentGroupMember m = new StudentGroupMember();
        m.setStudentId(1L); m.setGroupId(GROUP_ID);
        when(groupMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m));

        StudentGroup group = new StudentGroup(); group.setId(GROUP_ID); group.setName("空组");
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);

        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, GROUP_ID, null, SUBJECT);

        assertTrue(((String) result.get("message")).contains("暂无题目"));
        verify(taskMapper, never()).insert(any());
        verify(taskQuestionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("教师不存在 → teacherIdObj=null, task.teacherId=null")
    void composeRemedialTask_teacherNotFound() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID))));

        Classes cls = new Classes(); cls.setId(CLASS_ID); cls.setClassName("高三1班");
        when(classesMapper.selectById(CLASS_ID)).thenReturn(cls);

        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<QuestionBank> qs = mutableList(question(1L, SUBJECT, null));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(qs);

        when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        Map<String, Object> result = teacherService.composeRemedialTask(TEACHER_USER_ID, null, CLASS_ID, SUBJECT);

        assertTrue(result.containsKey("taskId"));
        ArgumentCaptor<Task> cap = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(cap.capture());
        assertNull(cap.getValue().getTeacherId());
    }

    @Test
    @DisplayName("TaskQuestion sortOrder 从0递增")
    void composeRemedialTask_sortOrderSequential() {
        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID))));

        Classes cls = new Classes(); cls.setId(CLASS_ID); cls.setClassName("高三1班");
        when(classesMapper.selectById(CLASS_ID)).thenReturn(cls);

        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<QuestionBank> qs = mutableList(question(10L, SUBJECT, null), question(20L, SUBJECT, null));
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(qs);

        stubTeacherLookup();
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        when(taskQuestionMapper.insert(any(TaskQuestion.class))).thenReturn(1);

        teacherService.composeRemedialTask(TEACHER_USER_ID, null, CLASS_ID, SUBJECT);

        ArgumentCaptor<TaskQuestion> cap = ArgumentCaptor.forClass(TaskQuestion.class);
        verify(taskQuestionMapper, times(2)).insert(cap.capture());
        List<TaskQuestion> all = cap.getAllValues();
        assertEquals(0, all.get(0).getSortOrder());
        assertEquals(1, all.get(1).getSortOrder());
    }

    // ═══════════════════════════════════════════
    //  teacherWeakTop tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("teacherWeakTop: 正常返回弱点排名")
    void teacherWeakTop_normal() {
        com.school.teaching.entity.Teacher t = new com.school.teaching.entity.Teacher();
        t.setId(TEACHER_ID); t.setUserId(TEACHER_USER_ID);
        when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);

        // subjectMapper used by getPrecisionSubjects() via getTeacherClassIds
        com.school.teaching.entity.DictSubject ds = new com.school.teaching.entity.DictSubject();
        ds.setSubjectName(SUBJECT);
        when(subjectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ds));

        TeacherClass tc = new TeacherClass();
        tc.setTeacherId(TEACHER_ID); tc.setClassId(CLASS_ID); tc.setSubject(SUBJECT);
        when(teacherClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tc));

        when(classesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        when(studentMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>(List.of(student(1L, CLASS_ID))));

        List<PrecisionProgress> weak = mutableList(progress(1L, 10L, 30), progress(1L, 20L, 40));
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(weak);

        KnowledgeNode kn10 = new KnowledgeNode(); kn10.setId(10L); kn10.setName("函数");
        KnowledgeNode kn20 = new KnowledgeNode(); kn20.setId(20L); kn20.setName("数列");
        when(nodeMapper.selectBatchIds(anySet())).thenReturn(List.of(kn10, kn20));

        List<Map<String, Object>> top = teacherService.teacherWeakTop(TEACHER_USER_ID, SUBJECT, 5);

        assertEquals(2, top.size());
        // errorCount = 1 per node (each has 1 student with that weak node)
        assertEquals("函数", top.get(0).get("name"));
        assertEquals(1, top.get(0).get("errorCount"));
        assertEquals("数列", top.get(1).get("name"));
        assertEquals(1, top.get(1).get("errorCount"));
    }

    @Test
    @DisplayName("teacherWeakTop: 教师无班级 → 空列表")
    void teacherWeakTop_noClasses() {
        com.school.teaching.entity.Teacher t = new com.school.teaching.entity.Teacher();
        t.setId(TEACHER_ID); t.setUserId(TEACHER_USER_ID);
        when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
        when(teacherClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(classesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> top = teacherService.teacherWeakTop(TEACHER_USER_ID, SUBJECT, 5);
        assertTrue(top.isEmpty());
    }
}
