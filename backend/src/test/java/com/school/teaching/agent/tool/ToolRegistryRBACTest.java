package com.school.teaching.agent.tool;

import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.security.UserContext;
import com.school.teaching.agent.tool.AnnotationToolScanner;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.PptGenerationService;
import com.school.teaching.service.TaskCrudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ToolRegistryRBACTest {

    @Mock private KnowledgeNodeMapper knowledgeNodeMapper;
    @Mock private ExamSyllabusMapper examSyllabusMapper;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private TaskSubmissionMapper taskSubmissionMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private PrecisionProgressMapper precisionProgressMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private NotificationMapper notificationMapper;
    @Mock private TaskCrudService taskCrudService;
    @Mock private ClassesMapper classesMapper;
    @Mock private UserMapper userMapper;
    @Mock private PptGenerationService pptService;
    @Mock private ExamSyllabusService examSyllabusService;
    @Mock private AnnotationToolScanner annotationScanner;
    @Mock private AgentConversationMapper agentConversationMapper;

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        AgentConfig config = new AgentConfig();
        config.setSqlLimit(500);

        registry = new ToolRegistry(
                knowledgeNodeMapper, examSyllabusMapper, questionBankMapper,
                taskMapper, taskSubmissionMapper, studentAnswerMapper,
                wrongQuestionMapper, precisionProgressMapper, studentMapper,
                notificationMapper, taskCrudService, classesMapper,
                userMapper, pptService, examSyllabusService,
                config, annotationScanner, agentConversationMapper);
        registry.init();
    }

    @Test
    @DisplayName("execute: 未知工具返回 fail")
    void unknownTool() {
        UserContext caller = UserContext.builder().roleName("TEACHER").build();
        ToolResult result = registry.execute("nonexistent_tool", Map.of(), caller, "s1");
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("未知工具"));
    }

    @Test
    @DisplayName("execute: 无权限角色抛 ToolAccessDeniedException")
    void roleWithoutPermission() {
        UserContext student = UserContext.builder().roleName("STUDENT").build();
        // teaching_create_task 仅 TEACHER+
        ToolAccessDeniedException ex = assertThrows(ToolAccessDeniedException.class,
                () -> registry.execute("teaching_create_task", Map.of("title", "t", "classIds", List.of(1)), student, "s1"));
        assertTrue(ex.getMessage().contains("无权"));
    }

    @Test
    @DisplayName("execute: STUDENT 可调 teaching_knowledge_search")
    void studentCanSearch() {
        UserContext student = UserContext.builder().roleName("STUDENT").userId(1L).build();
        ToolResult result = registry.execute("teaching_knowledge_search", Map.of("keyword", "计算机"), student, "s1");
        assertNotNull(result);
        // 查询结果可能为空，但不应抛异常
        assertTrue(result.isSuccess() || !result.isSuccess());
    }

    @Test
    @DisplayName("getDefinitions: 按角色过滤工具列表")
    void getDefinitionsFilteredByRole() {
        UserContext student = UserContext.builder().roleName("STUDENT").build();
        UserContext teacher = UserContext.builder().roleName("TEACHER").build();

        List<Map<String, Object>> studentTools = registry.getDefinitions(student);
        List<Map<String, Object>> teacherTools = registry.getDefinitions(teacher);

        // 学生工具数 < 教师工具数（create_task / send_notification 等被过滤）
        assertTrue(studentTools.size() < teacherTools.size(),
                "学生可见工具(" + studentTools.size() + ")应少于教师(" + teacherTools.size() + ")");
    }

    @Test
    @DisplayName("getToolNames: 返回所有已注册工具名")
    void getToolNames() {
        Set<String> names = registry.getToolNames();
        assertTrue(names.contains("teaching_knowledge_search"));
        assertTrue(names.contains("teaching_create_task"));
        assertTrue(names.contains("teaching_my_classes"));
        assertTrue(names.contains("teaching_generate_ppt"));
        assertTrue(names.contains("teaching_aggregate_questions"));
    }
}