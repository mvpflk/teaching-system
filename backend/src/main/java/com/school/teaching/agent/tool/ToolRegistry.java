package com.school.teaching.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.agent.security.UserContext;
import com.school.teaching.agent.tool.annotation.AgentTool;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.PptGenerationService;
import com.school.teaching.service.TaskCrudService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, RegisteredTool> registry = new LinkedHashMap<>();
    private final ObjectMapper om;

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ExamSyllabusMapper examSyllabusMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final PrecisionProgressMapper precisionProgressMapper;
    private final StudentMapper studentMapper;
    private final NotificationMapper notificationMapper;
    private final TaskCrudService taskCrudService;
    private final ClassesMapper classesMapper;
    private final UserMapper userMapper;
    private final PptGenerationService pptService;
    private final ExamSyllabusService examSyllabusService;
    private final AgentConfig agentConfig;
    private final AnnotationToolScanner annotationScanner;
    private final AgentConversationMapper agentConversationMapper;

    public ToolRegistry(KnowledgeNodeMapper knowledgeNodeMapper,
                        ExamSyllabusMapper examSyllabusMapper,
                        QuestionBankMapper questionBankMapper,
                        TaskMapper taskMapper,
                        TaskSubmissionMapper taskSubmissionMapper,
                        StudentAnswerMapper studentAnswerMapper,
                        WrongQuestionMapper wrongQuestionMapper,
                        PrecisionProgressMapper precisionProgressMapper,
                        StudentMapper studentMapper,
                        NotificationMapper notificationMapper,
                        TaskCrudService taskCrudService,
                        ClassesMapper classesMapper,
                        UserMapper userMapper,
                        PptGenerationService pptService,
ExamSyllabusService examSyllabusService,
                         AgentConfig agentConfig,
                         AnnotationToolScanner annotationScanner,
                         AgentConversationMapper agentConversationMapper) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.examSyllabusMapper = examSyllabusMapper;
        this.questionBankMapper = questionBankMapper;
        this.taskMapper = taskMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.studentAnswerMapper = studentAnswerMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.precisionProgressMapper = precisionProgressMapper;
        this.studentMapper = studentMapper;
        this.notificationMapper = notificationMapper;
        this.taskCrudService = taskCrudService;
        this.classesMapper = classesMapper;
        this.userMapper = userMapper;
        this.pptService = pptService;
        this.examSyllabusService = examSyllabusService;
        this.agentConfig = agentConfig;
        this.annotationScanner = annotationScanner;
        this.agentConversationMapper = agentConversationMapper;
        this.om = new ObjectMapper();
    }

    @PostConstruct
    void init() {
        registerAllTools();
        registerAnnotatedTools();
    }

    private void registerAllTools() {
        register("teaching_knowledge_search", "搜索知识树中的知识点，返回层级路径、关联题目数和子节点列表",
                this::knowledgeSearch, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("keyword", param("string", "搜索关键词"), "subjectId", optionalParam("number", "学科ID（可选）")),
                "keyword");

        register("teaching_syllabus_lookup", "查询考纲要求（了解/理解/掌握/运用）和考试权重。支持 nodeId 精确查（推荐）、subject 模糊查、knowledgePoint 关键词过滤",
                this::syllabusLookup, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("nodeId", optionalParam("number", "知识点节点ID（精确查询，优先使用。如从knowledge_search结果中获取）"),
                       "subject", optionalParam("string", "学科名称（模糊搜索，与nodeId配合使用效果最佳）"),
                       "knowledgePoint", optionalParam("string", "知识点关键词（可选，进一步过滤结果）")));

        register("teaching_similar_questions", "搜索题库中同知识点、相近难度的练习题，返回题干、题型、难度",
                this::similarQuestions, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("nodeId", param("number", "知识点ID"), "difficulty", optionalParam("number", "难度1-5"), "count", optionalParam("number", "返回数量，默认5")),
                "nodeId");

        register("teaching_search_tasks", "按标题或班级搜索已有任务，返回任务列表（用于避重）",
                this::searchTasks, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("keyword", optionalParam("string", "任务标题关键词"), "classId", optionalParam("number", "班级ID")));

        register("teaching_create_task", "创建教学任务/作业，发布给学生。仅教师可用",
                this::createTask, Set.of("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN"),
                Map.of("title", param("string", "任务标题"), "taskType", optionalParam("string", "任务类型：HOMEWORK/EXAM/SURVEY，默认HOMEWORK"),
                        "classIds", param("array", "目标班级ID列表"), "description", optionalParam("string", "任务描述")),
                "title", "classIds");

        register("teaching_send_notification", "发送系统通知给指定班级或学生",
                this::sendNotification, Set.of("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN"),
                Map.of("title", param("string", "通知标题"), "content", param("string", "通知内容"),
                        "classIds", optionalParam("array", "班级ID列表"), "studentIds", optionalParam("array", "学生ID列表")),
                "title", "content");

        register("teaching_student_wrong_book", "查学生的错题本，返回错题列表、错误次数、是否已掌握。学生只能查自己的，教师查自己班级学生的",
                this::wrongBook, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("studentId", optionalParam("number", "学生ID。学生留空自动查自己，教师必填")));

        register("teaching_student_mastery", "查学生对某知识点的掌握度百分比和变化趋势。学生只能查自己的，教师查自己班级学生的",
                this::studentMastery, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("studentId", optionalParam("number", "学生ID"), "subject", optionalParam("string", "学科名称"), "nodeId", optionalParam("number", "知识点ID")));

        register("teaching_student_submissions", "查学生某次作业的提交详情（得分、用时、提交时间）。学生只能查自己的",
                this::studentSubmissions, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("studentId", optionalParam("number", "学生ID"), "taskId", optionalParam("number", "任务ID")));

        register("teaching_question_explain", "查题目的标准答案和详细解析，包含涉及的知识点",
                this::questionExplain, Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("questionId", param("number", "题目ID")),
                "questionId");

        register("teaching_class_analytics", "班级某次考试统计：均分、最高/最低分、及格率、各题得分率、各分数段人数",
                this::classAnalytics, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("classId", param("number", "班级ID"), "taskId", param("number", "考试任务ID")),
                "classId", "taskId");

        register("teaching_knowledge_trend", "某知识点全班掌握度变化趋势（多次考试对比）",
                this::knowledgeTrend, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("classId", param("number", "班级ID"), "nodeId", param("number", "知识点ID")),
                "classId", "nodeId");

        register("teaching_student_growth", "单个学生历次成绩曲线、积分变化、错题趋势",
                this::studentGrowth, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("studentId", param("number", "学生ID")),
                "studentId");

        register("teaching_my_classes", "查询当前教师任教的班级列表，含班级ID、名称、学生人数。教师无需传参，自动识别身份",
                this::myClasses, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of());

        register("teaching_class_students", "查询某班级的学生花名册（姓名、学号）。教师只能查自己任教的班级",
                this::classStudents, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("classId", param("number", "班级ID")),
                "classId");

        register("teaching_generate_ppt", "根据课题生成PPT课件（.pptx文件）。教师输入课题名称，自动生成包含封面、正文、小结的完整课件",
                this::generatePpt, Set.of("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN"),
                Map.of("topic", param("string", "课题名称"), "subject", optionalParam("string", "学科名称")),
                "topic");

        register("teaching_task_submission_status", "查询某次任务的提交情况：列出已提交和未提交的学生名单。用于教师追问[谁还没交作业]",
                this::taskSubmissionStatus, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("taskId", param("number", "任务ID")),
                "taskId");

        register("teaching_expand_node", "展开一个L2/L3节点，返回其下所有L4知识点列表（含ID、名称、考纲权重标识）。用于专题综合出题前的知识点收集",
                this::expandNode, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("nodeId", param("number", "要展开的节点ID（L2或L3均可）"),
                       "recursive", optionalParam("boolean", "是否递归展开到所有L4后代，默认true")),
                "nodeId");

        register("teaching_aggregate_questions", "跨多个知识点一次性聚合抽题。用于专题综合生成——教师说[出三角函数综合练习]时，先调expand_node展开所有L4节点，再调本工具聚合出题",
                this::aggregateQuestions, Set.of("TEACHER", "HEAD_TEACHER", "INSPECTOR", "ADMIN", "SUPER_ADMIN"),
                Map.of("nodeIds", param("array", "L4知识点ID列表（从teaching_expand_node获得）"),
                       "totalCount", optionalParam("number", "题目总数，默认10，上限50"),
                       "distribution", optionalParam("string", "分布方式：even(均分，默认) 或 weighted(按考纲权重HIGH/MEDIUM/LOW分配)"),
                       "difficulty", optionalParam("number", "难度筛选1-5，不传则不筛选")),
                "nodeIds");
    }

    /** 扫描并注册 @AgentTool 注解标注的工具，与手动注册共存 */
    private void registerAnnotatedTools() {
        List<AnnotatedToolDef> defs = annotationScanner.scan();
        for (AnnotatedToolDef def : defs) {
            ToolHandler handler = (args, caller) -> {
                try {
                    return (ToolResult) def.method.invoke(def.bean, args, caller);
                } catch (Exception e) {
                    log.error("注解工具执行异常: tool={}", def.name, e);
                    return ToolResult.fail("工具 " + def.name + " 执行异常");
                }
            };
            Map<String, Object> params = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : def.parameters.entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
            register(def.name, def.description, handler,
                    def.allowedRoles.isEmpty()
                            ? Set.of("STUDENT", "TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                            : def.allowedRoles,
                    params, def.requiredParams.toArray(new String[0]));
        }
        log.info("ToolRegistry: 注解工具注册完成，共 {} 个", defs.size());
    }

    private void register(String name, String desc, ToolHandler handler, Set<String> roles,
                          Map<String, Object> params, String... requiredParams) {
        RegisteredTool tool = new RegisteredTool();
        tool.name = name;
        tool.description = desc;
        tool.handler = handler;
        tool.allowedRoles = roles;
        tool.parameters = Map.of("type", "object", "properties", params,
                "required", List.of(requiredParams));
        registry.put(name, tool);
    }

    public ToolResult execute(String name, Map<String, Object> args, UserContext caller, String sessionId) {
        RegisteredTool tool = registry.get(name);
        if (tool == null) {
            return ToolResult.fail("未知工具: " + name);
        }
        if (!tool.allowedRoles.contains(caller.getRoleName())) {
            throw new ToolAccessDeniedException("角色 " + caller.getRoleName() + " 无权调用工具 " + name);
        }
        // A-4: 注入会话上下文，供 createTask 等工具使用
        Map<String, Object> enrichedArgs = new HashMap<>(args);
        enrichedArgs.put("_sessionId", sessionId);
        // 工具执行超时保护（15 秒），防止慢查询阻塞 Agent 循环
        try {
            return java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> tool.handler.execute(enrichedArgs, caller))
                    .get(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("工具执行超时: tool={}, userId={}", name, caller.getUserId());
            return ToolResult.fail("工具 " + name + " 执行超时，请简化查询条件后重试");
        } catch (Exception e) {
            log.error("工具执行异常: tool={}", name, e);
            return ToolResult.fail("工具执行异常: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getDefinitions(UserContext caller) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RegisteredTool tool : registry.values()) {
            if (tool.allowedRoles.contains(caller.getRoleName())) {
                list.add(tool.toDeepSeekFormat());
            }
        }
        return list;
    }

    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    // ======================== 工具实现 ========================

    private ToolResult knowledgeSearch(Map<String, Object> args, UserContext caller) {
        String keyword = (String) args.get("keyword");
        Number sid = (Number) args.get("subjectId");
        LambdaQueryWrapper<KnowledgeNode> q = new LambdaQueryWrapper<KnowledgeNode>()
                .like(KnowledgeNode::getName, keyword)
                .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                         .or().eq(KnowledgeNode::getStatus, "1")
                         .or().isNull(KnowledgeNode::getStatus));
        if (sid != null) {
            q.eq(KnowledgeNode::getSubjectId, sid.longValue());
        }
        q.orderByAsc(KnowledgeNode::getLevel).orderByAsc(KnowledgeNode::getSortOrder)
                .last("LIMIT " + agentConfig.getSqlLimit());
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(q);
        if (nodes.isEmpty()) {
            return ToolResult.ok("未找到匹配「" + keyword + "」的知识点。请尝试换一个关键词。");
        }
        // 格式化为 Markdown 层级树，LLM 可直接理解
        StringBuilder md = new StringBuilder();
        md.append("## 知识树查询结果（关键词：").append(keyword).append("）\n\n");
        // L2 模块
        List<KnowledgeNode> l2s = nodes.stream().filter(n -> n.getLevel() == 2).toList();
        if (!l2s.isEmpty()) {
            md.append("### L2 模块\n");
            for (KnowledgeNode n : l2s) {
                md.append("- **").append(n.getName()).append("**");
                if (n.getExamWeight() != null) md.append(" [").append(n.getExamWeight()).append("]");
                md.append("\n");
            }
            md.append("\n");
        }
        // L3/L4 子专题
        List<KnowledgeNode> subs = nodes.stream().filter(n -> n.getLevel() >= 3).toList();
        if (!subs.isEmpty()) {
            md.append("### 子专题/L4 知识点（共").append(subs.size()).append("个）\n");
            Map<Long, String> parentNames = new HashMap<>();
            for (KnowledgeNode n : nodes) {
                parentNames.put(n.getId(), n.getName());
            }
            for (KnowledgeNode n : subs) {
                String indent = n.getLevel() == 4 ? "  - " : "- ";
                md.append(indent).append(n.getName());
                if (n.getParentId() != null && parentNames.containsKey(n.getParentId())) {
                    md.append("  ← ").append(parentNames.get(n.getParentId()));
                }
                // 如果节点有教学内容，附带摘要
                if (n.getContent() != null && !n.getContent().isBlank()) {
                    String brief = n.getContent().length() > 120
                            ? n.getContent().substring(0, 120).replace('\n', ' ') + "…"
                            : n.getContent().replace('\n', ' ');
                    md.append("\n  📖 ").append(brief);
                }
                md.append("\n");
            }
        }
        if (nodes.size() >= agentConfig.getSqlLimit()) {
            md.append("\n> ⚠️ 结果已截断（超过").append(agentConfig.getSqlLimit()).append("条）。如需更精确结果，请使用更具体的关键词。");
        }
        return ToolResult.ok(md.toString());
    }

    private ToolResult syllabusLookup(Map<String, Object> args, UserContext caller) {
        Long nodeId = toLong(args.get("nodeId"));
        String subject = (String) args.get("subject");
        String kp = (String) args.get("knowledgePoint");

        List<ExamSyllabus> list;

        // 优先 nodeId 精确查询 —— 利用已有的 exam_syllabus_node_relation 表 + 祖先链遍历
        if (nodeId != null) {
            list = examSyllabusService.getSyllabiByNodeId(nodeId);
        } else if (subject != null && !subject.isBlank()) {
            // 回退：模糊 LIKE 搜索
            LambdaQueryWrapper<ExamSyllabus> q = new LambdaQueryWrapper<ExamSyllabus>()
                    .and(w -> w.like(ExamSyllabus::getTitle, subject)
                             .or().like(ExamSyllabus::getContent, subject));
            if (kp != null && !kp.isEmpty()) {
                q.and(w -> w.like(ExamSyllabus::getTitle, kp).or().like(ExamSyllabus::getContent, kp));
            }
            q.last("LIMIT 50");
            list = examSyllabusMapper.selectList(q);
        } else if (kp != null && !kp.isEmpty()) {
            LambdaQueryWrapper<ExamSyllabus> q = new LambdaQueryWrapper<ExamSyllabus>()
                    .and(w -> w.like(ExamSyllabus::getTitle, kp).or().like(ExamSyllabus::getContent, kp));
            q.last("LIMIT 50");
            list = examSyllabusMapper.selectList(q);
        } else {
            return ToolResult.fail("请提供 nodeId（推荐）或 subject 参数。例如：nodeId=1234 精确查某知识点的考纲要求，或 subject=\"数学\" 模糊搜索。");
        }
        if (list.isEmpty()) {
            return ToolResult.ok("未找到「" + subject + "」相关的考纲记录。");
        }
        StringBuilder md = new StringBuilder();
        md.append("## 考纲查询结果\n\n");
        for (ExamSyllabus s : list) {
            md.append("### ").append(s.getTitle() != null ? s.getTitle() : subject).append("\n");
            if (s.getContent() != null) {
                // 截断过长内容（最多 2000 字），同时保留了关键考察信息
                String content = s.getContent();
                if (content.length() > 2000) {
                    content = content.substring(0, 2000) + "\n\n> ⚠️ 考纲内容已截断。如需完整信息，请使用 knowledgePoint 参数指定具体知识点。";
                }
                md.append(content).append("\n\n");
            }
            if (s.getSyllabusMeta() != null && !s.getSyllabusMeta().isBlank()) {
                md.append("**结构化数据：** ").append(s.getSyllabusMeta()).append("\n");
            }
        }
        return ToolResult.ok(md.toString());
    }

    private ToolResult similarQuestions(Map<String, Object> args, UserContext caller) {
        Long nodeId = toLong(args.get("nodeId"));
        Integer difficulty = args.get("difficulty") != null ? ((Number) args.get("difficulty")).intValue() : null;
        int count = args.get("count") != null ? Math.min(((Number) args.get("count")).intValue(), 20) : 5;
        LambdaQueryWrapper<QuestionBank> q = new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getStatus, 1);
        if (nodeId != null) {
            q.eq(QuestionBank::getCategoryId, nodeId);
        }
        if (difficulty != null) {
            q.eq(QuestionBank::getDifficultyLevel, difficulty);
        }
        q.last("ORDER BY RAND() LIMIT " + count);
        List<QuestionBank> questions = questionBankMapper.selectList(q);
        return ToolResult.ok(questions.stream().map(qq -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", qq.getId());
            m.put("questionType", qq.getQuestionType());
            m.put("questionText", qq.getQuestionText());
            m.put("difficulty", qq.getDifficultyLevel());
            m.put("subject", qq.getSubject());
            m.put("categoryId", qq.getCategoryId());
            return m;
        }).collect(Collectors.toList()));
    }

    private ToolResult searchTasks(Map<String, Object> args, UserContext caller) {
        String keyword = (String) args.get("keyword");
        Number cid = (Number) args.get("classId");
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>();
        if (keyword != null && !keyword.isEmpty()) {
            q.like(Task::getTitle, keyword);
        }
        // 教师自动限定为任教班级的任务（task.target_id 是单班级外键）
        if (cid != null) {
            q.eq(Task::getTargetId, cid.longValue());
        } else if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                && !caller.getAccessibleClassIds().isEmpty()) {
            q.in(Task::getTargetId, caller.getAccessibleClassIds());
        }
        q.orderByDesc(Task::getCreatedAt).last("LIMIT 50");
        List<Task> tasks = taskMapper.selectList(q);
        return ToolResult.ok(tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("taskType", t.getTaskType());
            m.put("status", t.getStatus());
            m.put("deadline", t.getDeadline() != null ? t.getDeadline().toString() : "");
            m.put("classId", t.getTargetId());
            m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList()));
    }

    @Transactional
    private ToolResult createTask(Map<String, Object> args, UserContext caller) {
        String title = (String) args.get("title");
        String taskType = args.get("taskType") != null ? (String) args.get("taskType") : "HOMEWORK";
        String description = (String) args.get("description");
        @SuppressWarnings("unchecked")
        List<Number> classIds = (List<Number>) args.get("classIds");

        if (caller.isTeacher() && classIds != null && caller.getAccessibleClassIds() != null) {
            for (Number cid : classIds) {
                if (!caller.getAccessibleClassIds().contains(cid.longValue())) {
                    throw new ToolAccessDeniedException("您不任教班级ID=" + cid + "，无法为其创建任务");
                }
            }
        }

        Task task = new Task();
        task.setTitle(title);
        task.setTaskType(taskType);
        task.setDescription(description);
        task.setTeacherId(caller.getTeacherId());
        task.setSchoolId(caller.getSchoolId() != null ? caller.getSchoolId() : 1L);
        task.setStatus("DRAFT");
        taskCrudService.create(task);

        // A-4: 关联会话与任务
        String sessionId = (String) args.get("_sessionId");
        if (sessionId != null) {
            linkTaskToConversation(sessionId, task.getId());
        }

        return ToolResult.ok(Map.of("taskId", task.getId(), "title", task.getTitle(), "status", task.getStatus(),
                "message", "任务创建成功，可在系统中编辑和发布"));
    }

    /** A-4: 将会话与任务关联，支持学习效果闭环追踪 */
    private void linkTaskToConversation(String sessionId, Long taskId) {
        LambdaQueryWrapper<AgentConversation> qw = new LambdaQueryWrapper<AgentConversation>()
                .eq(AgentConversation::getSessionId, sessionId);
        AgentConversation conv = agentConversationMapper.selectOne(qw.last("LIMIT 1"));
        if (conv != null) {
            conv.setFollowUpTaskId(taskId);
            agentConversationMapper.updateById(conv);
        }
    }

    @Transactional
    private ToolResult sendNotification(Map<String, Object> args, UserContext caller) {
        String title = (String) args.get("title");
        String content = (String) args.get("content");
        @SuppressWarnings("unchecked")
        List<Number> classIds = (List<Number>) args.get("classIds");
        @SuppressWarnings("unchecked")
        List<Number> studentIds = (List<Number>) args.get("studentIds");

        if (caller.isTeacher() && classIds != null && caller.getAccessibleClassIds() != null) {
            for (Number cid : classIds) {
                if (!caller.getAccessibleClassIds().contains(cid.longValue())) {
                    throw new ToolAccessDeniedException("您不任教班级ID=" + cid + "，无法向其发送通知");
                }
            }
        }

        // 收集所有目标用户ID（批量查询替代 N+1）
        Set<Long> targetUserIds = new HashSet<>();
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Long> ids = studentIds.stream().map(Number::longValue).toList();
            List<Student> students = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getId, ids));
            for (Student s : students) {
                if (s.getUserId() != null) targetUserIds.add(s.getUserId());
            }
        }
        if (classIds != null && !classIds.isEmpty()) {
            List<Long> ids = classIds.stream().map(Number::longValue).toList();
            List<Student> students = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getClassId, ids));
            for (Student s : students) {
                if (s.getUserId() != null) targetUserIds.add(s.getUserId());
            }
        }

        if (targetUserIds.isEmpty()) {
            return ToolResult.fail("未指定任何通知接收者");
        }

        // 为每个目标用户创建独立的通知记录
        int count = 0;
        for (Long uid : targetUserIds) {
            Notification notif = new Notification();
            notif.setTitle(title);
            notif.setContent(content);
            notif.setType("SYSTEM");
            notif.setUserId(uid);
            notificationMapper.insert(notif);
            count++;
        }

        return ToolResult.ok(Map.of("message", "通知已发送给 " + count + " 人"));
    }

    // ======================== P2 工具实现 ========================

    private ToolResult wrongBook(Map<String, Object> args, UserContext caller) {
        Long studentId = resolveStudentId(args, caller);
        if (studentId == null) return ToolResult.fail("无法确定学生ID");

        LambdaQueryWrapper<WrongQuestion> q = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .last("LIMIT " + agentConfig.getSqlLimit());
        List<WrongQuestion> list = wrongQuestionMapper.selectList(q);
        return ToolResult.ok(list.stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId()); m.put("questionId", w.getQuestionId());
            m.put("wrongCount", w.getWrongCount()); m.put("isMastered", w.getIsMastered());
            m.put("lastWrongTime", w.getLastWrongTime() != null ? w.getLastWrongTime().toString() : "");
            return m;
        }).collect(Collectors.toList()));
    }

    private ToolResult studentMastery(Map<String, Object> args, UserContext caller) {
        Long studentId = resolveStudentId(args, caller);
        if (studentId == null) return ToolResult.fail("无法确定学生ID");
        String subject = (String) args.get("subject");
        Number nodeIdNum = (Number) args.get("nodeId");

        LambdaQueryWrapper<PrecisionProgress> q = new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId);
        if (subject != null && !subject.isEmpty()) q.eq(PrecisionProgress::getSubject, subject);
        if (nodeIdNum != null) q.eq(PrecisionProgress::getNodeId, nodeIdNum.longValue());
        q.orderByAsc(PrecisionProgress::getMasteryPercent).last("LIMIT 100");

        List<PrecisionProgress> list = precisionProgressMapper.selectList(q);
        if (list.isEmpty()) {
            return ToolResult.ok("该学生暂无掌握度记录。");
        }
        // 格式化为可读的 Markdown 表格
        StringBuilder md = new StringBuilder();
        md.append("## 学生掌握度数据\n\n");
        md.append("| 学科 | 掌握度 | 练习次数 | 正确次数 | 水平判定 |\n");
        md.append("|------|--------|----------|----------|----------|\n");
        int weakCount = 0, mediumCount = 0, goodCount = 0;
        for (PrecisionProgress p : list) {
            int mastery = p.getMasteryPercent() != null ? p.getMasteryPercent().intValue() : 0;
            String level;
            if (mastery < 30) { level = "起步期"; weakCount++; }
            else if (mastery < 60) { level = "成长中"; weakCount++; }
            else if (mastery < 85) { level = "已达标"; mediumCount++; }
            else { level = "已掌握"; goodCount++; }
            md.append("| ").append(p.getSubject() != null ? p.getSubject() : "—")
              .append(" | ").append(mastery).append("%")
              .append(" | ").append(p.getTotalAttempts() != null ? p.getTotalAttempts() : 0)
              .append(" | ").append(p.getTotalCorrect() != null ? p.getTotalCorrect() : 0)
              .append(" | ").append(level).append(" |\n");
        }
        md.append("\n**汇总**：起步/成长 ").append(weakCount).append("个 | 已达标 ")
          .append(mediumCount).append("个 | 已掌握 ").append(goodCount).append("个\n");
        if (weakCount > 0) {
            md.append("\n💡 掌握度低于60%的知识点建议用最基础的方式讲解，给出详细示例。");
        }
        return ToolResult.ok(md.toString());
    }

    private ToolResult studentSubmissions(Map<String, Object> args, UserContext caller) {
        Long studentId = resolveStudentId(args, caller);
        if (studentId == null) return ToolResult.fail("无法确定学生ID");
        Number taskIdNum = (Number) args.get("taskId");

        LambdaQueryWrapper<TaskSubmission> q = new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .orderByDesc(TaskSubmission::getSubmittedAt)
                .last("LIMIT 20");
        if (taskIdNum != null) q.eq(TaskSubmission::getTaskId, taskIdNum.longValue());

        List<TaskSubmission> list = taskSubmissionMapper.selectList(q);
        return ToolResult.ok(list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId()); m.put("taskId", s.getTaskId());
            m.put("totalScore", s.getScore()); m.put("score", s.getScore());
            m.put("attemptNumber", 1);
            m.put("submitTime", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "");
            return m;
        }).collect(Collectors.toList()));
    }

    private ToolResult questionExplain(Map<String, Object> args, UserContext caller) {
        Long questionId = toLong(args.get("questionId"));
        if (questionId == null) return ToolResult.fail("题目ID不能为空");

        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) return ToolResult.fail("题目不存在");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId()); m.put("questionType", q.getQuestionType());
        m.put("questionText", q.getQuestionText()); m.put("options", q.getOptions());
        m.put("correctAnswer", q.getCorrectAnswer()); m.put("explanation", q.getExplanation());
        m.put("difficulty", q.getDifficultyLevel()); m.put("subject", q.getSubject());
        return ToolResult.ok(m);
    }

    private ToolResult classAnalytics(Map<String, Object> args, UserContext caller) {
        Long classId = toLong(args.get("classId"));
        Long taskId = toLong(args.get("taskId"));
        if (classId == null || taskId == null) return ToolResult.fail("班级ID和任务ID不能为空");

        if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                && !caller.getAccessibleClassIds().contains(classId)) {
            throw new ToolAccessDeniedException("您不任教该班级，无法查看其数据");
        }

        LambdaQueryWrapper<TaskSubmission> q = new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .last("LIMIT " + agentConfig.getSqlLimit());
        List<TaskSubmission> subs = taskSubmissionMapper.selectList(q);
        if (subs.isEmpty()) return ToolResult.ok(Map.of("message", "该任务暂无提交数据"));

        DoubleSummaryStatistics stats = subs.stream()
                .mapToDouble(s -> s.getScore() != null ? s.getScore().doubleValue() : 0)
                .summaryStatistics();
        double avg = stats.getAverage();
        double max = stats.getMax();
        double min = stats.getMin();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submitCount", subs.size()); result.put("avgScore", Math.round(avg * 100.0) / 100.0);
        result.put("maxScore", max); result.put("minScore", min);
        return ToolResult.ok(result);
    }

    private ToolResult knowledgeTrend(Map<String, Object> args, UserContext caller) {
        Long nodeId = toLong(args.get("nodeId"));
        if (nodeId == null) return ToolResult.fail("知识点ID不能为空");

        LambdaQueryWrapper<StudentAnswer> q = new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getQuestionId, nodeId)
                .orderByAsc(StudentAnswer::getCreateTime)
                .last("LIMIT " + agentConfig.getSqlLimit());
        List<StudentAnswer> answers = studentAnswerMapper.selectList(q);
        return ToolResult.ok(Map.of("nodeId", nodeId, "answerCount", answers.size(),
                "message", "知识点作答数据已加载，请在对话中进一步分析趋势"));
    }

    private ToolResult studentGrowth(Map<String, Object> args, UserContext caller) {
        Long studentId = toLong(args.get("studentId"));
        if (studentId == null) return ToolResult.fail("学生ID不能为空");

        Student student = studentMapper.selectById(studentId);
        if (student == null) return ToolResult.fail("学生不存在");

        if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                && !caller.getAccessibleClassIds().contains(student.getClassId())) {
            throw new ToolAccessDeniedException("您不任教该学生所在班级，无法查看其数据");
        }

        List<TaskSubmission> subs = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getStudentId, studentId)
                                .orderByAsc(TaskSubmission::getSubmittedAt)
                        .last("LIMIT 50"));
        return ToolResult.ok(subs.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", s.getTaskId()); m.put("score", s.getScore());
            m.put("totalScore", s.getScore()); m.put("attemptNumber", 1);
            m.put("submitTime", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "");
            return m;
        }).collect(Collectors.toList()));
    }

    private ToolResult myClasses(Map<String, Object> args, UserContext caller) {
        if (caller.getAccessibleClassIds() == null || caller.getAccessibleClassIds().isEmpty()) {
            return ToolResult.ok("你当前未关联任何班级。请联系管理员在系统中为你分配任教班级。");
        }
        List<Classes> classes = classesMapper.selectList(
                new LambdaQueryWrapper<Classes>().in(Classes::getId, caller.getAccessibleClassIds()));

        // 一次 GROUP BY 查询统计各班学生数（避免 N+1）
        Map<Long, Long> studentCounts = new HashMap<>();
        if (!classes.isEmpty()) {
            List<Long> cids = classes.stream().map(Classes::getId).collect(Collectors.toList());
            List<Map<String, Object>> countRows = studentMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Student>()
                            .select("class_id", "COUNT(*) as cnt")
                            .in("class_id", cids)
                            .groupBy("class_id"));
            for (Map<String, Object> row : countRows) {
                Object cidObj = row.get("class_id");
                Object cntObj = row.get("cnt");
                if (cidObj != null && cntObj != null) {
                    studentCounts.put(((Number) cidObj).longValue(), ((Number) cntObj).longValue());
                }
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Classes c : classes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getId());
            m.put("className", c.getClassName());
            m.put("grade", c.getGrade());
            m.put("major", c.getMajor());
            m.put("studentCount", studentCounts.getOrDefault(c.getId(), 0L));
            m.put("isHeadTeacher", caller.isHeadTeacher() && c.getHeadTeacherId() != null
                    && c.getHeadTeacherId().equals(caller.getTeacherId()));
            list.add(m);
        }
        return ToolResult.ok(Map.of("classes", list, "totalClasses", list.size(),
                "message", "以上是你任教的全部班级。当学生/家长提及[我班/我们班]而未指定班级时，优先从以上列表匹配。"));
    }

    private ToolResult classStudents(Map<String, Object> args, UserContext caller) {
        Long classId = toLong(args.get("classId"));
        if (classId == null) return ToolResult.fail("班级ID不能为空。请先调用 teaching_my_classes 获取你的班级列表。");

        if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                && !caller.getAccessibleClassIds().contains(classId)) {
            throw new ToolAccessDeniedException("你不任教班级ID=" + classId + "，无法查看其学生名单");
        }

        Classes cls = classesMapper.selectById(classId);
        String className = cls != null ? cls.getClassName() : "班级" + classId;

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        Map<Long, String> nameMap = resolveStudentNames(students);

        List<Map<String, Object>> list = students.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", s.getId());
            m.put("name", nameMap.getOrDefault(s.getUserId(), "未知"));
            m.put("studentNo", s.getStudentNumber());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classId", classId);
        result.put("className", className);
        result.put("totalStudents", list.size());
        result.put("students", list);
        return ToolResult.ok(result);
    }

    private ToolResult taskSubmissionStatus(Map<String, Object> args, UserContext caller) {
        Long taskId = toLong(args.get("taskId"));
        if (taskId == null) return ToolResult.fail("任务ID不能为空。请先调用 teaching_search_tasks 查找任务。");

        Task task = taskMapper.selectById(taskId);
        if (task == null) return ToolResult.fail("任务ID=" + taskId + " 不存在");

        Long taskClassId = task.getTargetId();
        if (taskClassId == null) {
            return ToolResult.fail("该任务未关联任何班级（targetId 为空）");
        }

        // 教师权限检查
        if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                && !caller.getAccessibleClassIds().contains(taskClassId)) {
            throw new ToolAccessDeniedException("你不任教该任务关联的班级，无法查看提交状态");
        }

        // 获取班级信息和学生名单
        Classes cls = classesMapper.selectById(taskClassId);
        String className = cls != null ? cls.getClassName() : "班级" + taskClassId;

        List<Student> allStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, taskClassId));
        Map<Long, String> nameMap = resolveStudentNames(allStudents);

        // 查询已提交的学生
        List<TaskSubmission> submissions = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, taskId));
        Set<Long> submittedStudentIds = submissions.stream()
                .map(TaskSubmission::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 分组：已提交 vs 未提交
        List<Map<String, Object>> submitted = new ArrayList<>();
        List<Map<String, Object>> notSubmitted = new ArrayList<>();
        for (Student s : allStudents) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", s.getId());
            m.put("name", nameMap.getOrDefault(s.getUserId(), "未知"));
            m.put("studentNo", s.getStudentNumber());
            if (submittedStudentIds.contains(s.getId())) {
                TaskSubmission sub = submissions.stream()
                        .filter(sb -> sb.getStudentId() != null && sb.getStudentId().equals(s.getId()))
                        .findFirst().orElse(null);
                m.put("score", sub != null && sub.getScore() != null ? sub.getScore() : null);
                m.put("submittedAt", sub != null && sub.getSubmittedAt() != null
                        ? sub.getSubmittedAt().toString() : "");
                submitted.add(m);
            } else {
                notSubmitted.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("taskTitle", task.getTitle());
        result.put("classId", taskClassId);
        result.put("className", className);
        result.put("deadline", task.getDeadline() != null ? task.getDeadline().toString() : "无截止日期");
        result.put("totalStudents", allStudents.size());
        result.put("submittedCount", submitted.size());
        result.put("notSubmittedCount", notSubmitted.size());
        result.put("submitted", submitted);
        result.put("notSubmitted", notSubmitted);
        result.put("message", String.format("%s：共%d名学生，已提交%d人，未提交%d人。",
                className, allStudents.size(), submitted.size(), notSubmitted.size()));
        return ToolResult.ok(result);
    }

    private ToolResult generatePpt(Map<String, Object> args, UserContext caller) {
        String topic = (String) args.get("topic");
        String subject = (String) args.get("subject");
        if (topic == null || topic.isBlank()) return ToolResult.fail("课题名称不能为空");

        try {
            String filePath = pptService.generate(topic, subject, null);
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            return ToolResult.ok("✅ PPT已生成！\n\n📥 [点击下载：" + topic + ".pptx](/api/agent/ppt/download?file=" + fileName + ")\n\n文件名：" + fileName);
        } catch (Exception e) {
            log.error("PPT生成失败: topic={}", topic, e);
            return ToolResult.fail("PPT生成失败: " + e.getMessage());
        }
    }

    // ======================== 专题综合生成工具 ========================

    /** 展开父节点到所有L4子孙知识点 */
    private ToolResult expandNode(Map<String, Object> args, UserContext caller) {
        Long nodeId = toLong(args.get("nodeId"));
        if (nodeId == null) return ToolResult.fail("nodeId 不能为空");

        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) return ToolResult.fail("节点不存在: " + nodeId);

        boolean recursive = args.get("recursive") == null
                || !"false".equalsIgnoreCase(String.valueOf(args.get("recursive")));

        List<KnowledgeNode> allL4 = new ArrayList<>();
        if (recursive) {
            collectL4Descendants(nodeId, allL4);
        } else {
            allL4 = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getParentId, nodeId)
                            .eq(KnowledgeNode::getLevel, 4)
                            .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                                     .or().isNull(KnowledgeNode::getStatus))
                            .orderByAsc(KnowledgeNode::getSortOrder));
        }

        if (allL4.isEmpty()) {
            return ToolResult.ok("节点「" + node.getName() + "」下未找到L4知识点。该节点层级为L" + node.getLevel()
                    + "，可能需要选择一个L2或L3父节点。");
        }

        StringBuilder md = new StringBuilder();
        md.append("## 节点展开：").append(node.getName()).append("\n\n");
        md.append("共 ").append(allL4.size()).append(" 个L4知识点：\n\n");
        for (KnowledgeNode l4 : allL4) {
            md.append("- **").append(l4.getName()).append("** (ID=").append(l4.getId()).append(")");
            if (l4.getExamWeight() != null) {
                md.append(" `").append(l4.getExamWeight()).append("`");
            }
            md.append("\n");
        }
        md.append("\n> 可将上述ID列表传入 teaching_aggregate_questions 聚合出题。");
        return ToolResult.ok(md.toString());
    }

    /** 跨多个L4节点聚合出题 */
    private ToolResult aggregateQuestions(Map<String, Object> args, UserContext caller) {
        @SuppressWarnings("unchecked")
        List<Number> nodeIdsRaw = (List<Number>) args.get("nodeIds");
        if (nodeIdsRaw == null || nodeIdsRaw.isEmpty()) {
            return ToolResult.fail("nodeIds 不能为空，请先从 teaching_expand_node 获取知识点ID列表");
        }
        List<Long> nodeIds = nodeIdsRaw.stream().map(Number::longValue).distinct().toList();

        int totalCount = args.get("totalCount") != null
                ? Math.min(((Number) args.get("totalCount")).intValue(), 50) : 10;
        String distribution = args.get("distribution") != null
                ? (String) args.get("distribution") : "even";
        Integer difficulty = args.get("difficulty") != null
                ? ((Number) args.get("difficulty")).intValue() : null;

        // 计算每节点分配题数
        Map<Long, Integer> allocation = allocateQuestionCounts(nodeIds, totalCount, distribution);

        // 按节点聚合
        List<Map<String, Object>> allQuestions = new ArrayList<>();

        // E-3: 批量查询替代 N+1 — 一次查出所有节点 + 所有题目
        Map<Long, KnowledgeNode> nodeMap;
        if (nodeIds.size() == 1) {
            KnowledgeNode single = knowledgeNodeMapper.selectById(nodeIds.get(0));
            nodeMap = single != null ? Map.of(nodeIds.get(0), single) : Collections.emptyMap();
        } else {
            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>().in(KnowledgeNode::getId, nodeIds));
            nodeMap = nodes.stream().collect(Collectors.toMap(KnowledgeNode::getId, node -> node));
        }

        LambdaQueryWrapper<QuestionBank> batchQ = new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getCategoryId, nodeIds)
                .eq(QuestionBank::getStatus, 1);
        if (difficulty != null) batchQ.eq(QuestionBank::getDifficultyLevel, difficulty);
        List<QuestionBank> allNodeQuestions = questionBankMapper.selectList(batchQ);
        Map<Long, List<QuestionBank>> questionsByNode = allNodeQuestions.stream()
                .collect(Collectors.groupingBy(QuestionBank::getCategoryId));

        for (Map.Entry<Long, Integer> entry : allocation.entrySet()) {
            Long nodeId = entry.getKey();
            int count = entry.getValue();
            if (count <= 0) continue;

            List<QuestionBank> nodeQuestions = questionsByNode.getOrDefault(nodeId, Collections.emptyList());
            if (nodeQuestions.isEmpty()) continue;

            String nodeName = nodeMap.containsKey(nodeId) ? nodeMap.get(nodeId).getName() : "节点" + nodeId;

            // 随机选取 count 道（或全部）
            Collections.shuffle(nodeQuestions, new java.util.Random());
            List<QuestionBank> picked = nodeQuestions.subList(0, Math.min(count, nodeQuestions.size()));

            for (QuestionBank qq : picked) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", qq.getId());
                m.put("questionType", qq.getQuestionType());
                m.put("questionText", qq.getQuestionText());
                m.put("difficulty", qq.getDifficultyLevel());
                m.put("sourceNodeId", nodeId);
                m.put("sourceNodeName", nodeName);
                allQuestions.add(m);
            }
        }

        if (allQuestions.isEmpty()) {
            return ToolResult.ok("所选 " + nodeIds.size() + " 个知识点暂无匹配题目。"
                    + (difficulty != null ? "可尝试去掉难度筛选（difficulty不传）。" : ""));
        }

        return ToolResult.ok(Map.of(
                "totalCount", allQuestions.size(),
                "requestedCount", totalCount,
                "distribution", distribution,
                "nodeCount", nodeIds.size(),
                "questions", allQuestions
        ));
    }

    /** 递归收集L4子孙节点（遍历所有层级<4的中间节点，直到L4） */
    private void collectL4Descendants(Long parentId, List<KnowledgeNode> result) {
        // 广度优先批量查询，替代递归逐层查询（N+1 → 最多 4 次 DB 查询）
        List<Long> currentLevel = new ArrayList<>();
        currentLevel.add(parentId);
        int maxDepth = 5; // 安全上限：不会超过 L1→L2→L3→L4→L5

        for (int depth = 0; depth < maxDepth && !currentLevel.isEmpty(); depth++) {
            List<KnowledgeNode> children = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .in(KnowledgeNode::getParentId, currentLevel)
                            .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                                     .or().isNull(KnowledgeNode::getStatus))
                            .orderByAsc(KnowledgeNode::getSortOrder));

            currentLevel.clear();
            for (KnowledgeNode child : children) {
                if (child.getLevel() != null && child.getLevel() == 4) {
                    result.add(child);
                } else if (child.getLevel() != null && child.getLevel() < 4) {
                    currentLevel.add(child.getId());
                }
            }
        }
    }

    /** 按分布策略给每个L4节点分配题数。保证每个节点≥1题（若还有余量），总数不超过total */
    private Map<Long, Integer> allocateQuestionCounts(List<Long> nodeIds, int total, String distribution) {
        Map<Long, Integer> alloc = new LinkedHashMap<>();
        int n = nodeIds.size();
        if (n == 0) return alloc;

        if ("weighted".equals(distribution)) {
            // E-3: 批量查询所有节点权重替代 N+1
            Map<Long, Integer> weights = new LinkedHashMap<>();
            List<KnowledgeNode> nodes;
            if (nodeIds.size() == 1) {
                KnowledgeNode single = knowledgeNodeMapper.selectById(nodeIds.get(0));
                nodes = single != null ? List.of(single) : Collections.emptyList();
            } else {
                nodes = knowledgeNodeMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeNode>().in(KnowledgeNode::getId, nodeIds));
            }
            Map<Long, KnowledgeNode> nodeMap = nodes.stream()
.collect(Collectors.toMap(KnowledgeNode::getId, node -> node));
            int totalWeight = 0;
            for (Long id : nodeIds) {
                KnowledgeNode node = nodeMap.get(id);
                String ew = node != null ? node.getExamWeight() : null;
                int w = switch (ew) {
                    case "HIGH" -> 3; case "MEDIUM" -> 2; case "LOW" -> 1; default -> 2;
                };
                weights.put(id, w);
                totalWeight += w;
            }
            int remaining = total;
            for (int i = 0; i < nodeIds.size(); i++) {
                Long id = nodeIds.get(i);
                int w = weights.get(id);
                int cnt;
                if (remaining <= 0) {
                    cnt = 0;
                } else if (i == nodeIds.size() - 1) {
                    cnt = remaining; // 最后一个节点拿走所有剩余
                } else {
                    int proportional = Math.max(1, (int) Math.round((double) total * w / totalWeight));
                    cnt = Math.min(proportional, remaining); // 不能超支
                }
                alloc.put(id, cnt);
                remaining -= cnt;
            }
        } else {
            // even: 均分。base题每人都有，remainder题分给前几个节点
            int base = total / n;
            int remainder = total % n;
            int given = 0;
            for (Long id : nodeIds) {
                int cnt = base + (remainder > 0 ? 1 : 0);
                if (remainder > 0) remainder--;
                // 保证每节点至少1题（前提是total允许），但不超total
                cnt = Math.min(cnt, total - given);
                if (cnt == 0 && given < total) cnt = 1; // 还有余量就至少给1题
                alloc.put(id, cnt);
                given += cnt;
                if (given >= total) break;
            }
        }
        return alloc;
    }

    // ======================== 辅助方法 ========================

    /** 根据学生列表批量查询姓名（通过 userId → User.realName） */
    private Map<Long, String> resolveStudentNames(List<Student> students) {
        List<Long> userIds = students.stream()
                .map(Student::getUserId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, String> nameMap = new HashMap<>();
        for (User u : users) {
            nameMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername());
        }
        return nameMap;
    }

    private Long resolveStudentId(Map<String, Object> args, UserContext caller) {
        Number sid = (Number) args.get("studentId");
        if (sid != null) {
            long targetId = sid.longValue();
            if (caller.isStudent() && targetId != caller.getStudentId()) {
                throw new ToolAccessDeniedException("学生只能查看自己的数据");
            }
            // 教师：校验目标学生在自己任教班级内（防越权查其他班级学生）
            if (caller.isTeacher() && caller.getAccessibleClassIds() != null
                    && !caller.getAccessibleClassIds().isEmpty()) {
                Student targetStudent = studentMapper.selectById(targetId);
                if (targetStudent == null || targetStudent.getClassId() == null
                        || !caller.getAccessibleClassIds().contains(targetStudent.getClassId())) {
                    throw new ToolAccessDeniedException("你不任教该学生所在班级，无法查看其数据");
                }
            }
            return targetId;
        }
        if (caller.isStudent()) return caller.getStudentId();
        return null;
    }

    private Map<String, Object> param(String type, String desc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private Map<String, Object> optionalParam(String type, String desc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class RegisteredTool {
        String name;
        String description;
        ToolHandler handler;
        Set<String> allowedRoles;
        Map<String, Object> parameters;

        Map<String, Object> toDeepSeekFormat() {
            return Map.of(
                    "type", "function",
                    "function", Map.of("name", name, "description", description, "parameters", parameters)
            );
        }
    }
}
