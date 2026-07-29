package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.ClassroomQuestion;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.Task;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.AiQuestionGeneratorService;
import com.school.teaching.service.CardRecommendationService;
import com.school.teaching.service.ClassService;
import com.school.teaching.service.ClassroomService;
import com.school.teaching.service.StudentService;
import com.school.teaching.service.TaskService;
import com.school.teaching.service.TaskSubmissionService;
import com.school.teaching.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/classroom")
@Slf4j
public class ClassroomController {

    @Autowired private ClassroomService classroomService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private AiQuestionGeneratorService aiQuestionGeneratorService;
    @Autowired private com.school.teaching.service.AiContentGeneratorService aiContentGeneratorService;
    @Autowired private CardRecommendationService cardRecommendationService;
    @Autowired private ClassService classService;
    @Autowired private TaskService taskService;
    @Autowired private StudentService studentService;
    @Autowired private TaskSubmissionService taskSubmissionService;
    @Autowired private TeacherService teacherService;

    /** 检查当前教师是否有权限操作指定班级 */
    private boolean canAccessClass(Long classId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return false;
        if (SecurityUtils.isAdmin() || SecurityUtils.isInspector()) return true;
        if (SecurityUtils.isTeacherOrAdmin()) {
            Classes c = classService.getClassById(classId);
            if (c != null && c.getHeadTeacherId() != null && c.getHeadTeacherId().equals(userId)) return true;
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            return teacherId != null && teacherService.isTeacherOfClass(teacherId, classId);
        }
        return false;
    }

    // S5: AI出题频率限制 — teacherId → 最近调用时间戳列表（使用 CopyOnWriteArrayList 保证线程安全）
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CopyOnWriteArrayList<Long>> aiGenRateLimiter = new java.util.concurrent.ConcurrentHashMap<>();

    // === SSE 订阅（学生端连接班级互动频道） ===
    @GetMapping("/class/{classId}/subscribe")
    public SseEmitter subscribe(@PathVariable Long classId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("未登录或Token无效"));
            } catch (IOException e) {
                log.warn("SSE send error: 未登录或Token无效, userId=null");
            }
            emitter.complete();
            return emitter;
        }
        // S2: 学生端校验班级归属，同时提取studentId用于connected事件告知前端身份
        Long studentId = null;
        if (!SecurityUtils.isTeacherOrAdmin()) {
            Student student = studentService.getStudentByUserId(userId);
            if (student == null || !classId.equals(student.getClassId())) {
                SseEmitter emitter = new SseEmitter(0L);
                try {
                    emitter.send(SseEmitter.event().name("error").data("无权访问该班级"));
                } catch (IOException e) {
                    log.warn("SSE send error: 无权访问该班级, classId={}, userId={}", classId, userId);
                }
                emitter.complete();
                return emitter;
            }
            studentId = student.getId();
        }
        return classroomService.subscribeClassroom(classId, userId, studentId);
    }

    // === 抽问 ===
    @PostMapping("/quiz/start")
    public R<Map<String, Object>> startQuiz(@Valid @RequestBody QuizStartRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "未找到教师信息");
        Map<Long, Double> weights = new HashMap<>();
        if (request.getStudentWeights() != null) {
            request.getStudentWeights().forEach((k, v) -> weights.put(Long.valueOf(k), ((Number) v).doubleValue()));
        }
        List<Long> excludeIds = request.getExcludeStudentIds() != null ? request.getExcludeStudentIds() : List.of();
        String sceneMode = request.getSceneMode() != null ? request.getSceneMode() : "LAB";
        return R.ok(classroomService.startQuiz(request.getClassId(), teacherId, request.getQuestionId(),
                request.getQuestionText(), sceneMode, request.getQuestionType(), request.getOptions(),
                excludeIds, weights));
    }

    /** 学生提交抽问文字答案 */
    @PostMapping("/sessions/{sessionId}/answer")
    public R<Map<String, Object>> submitAnswer(@PathVariable Long sessionId, @RequestBody QuizAnswerRequest request) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        String answerText = request.getAnswerText() != null ? request.getAnswerText() : "";
        return R.ok(classroomService.submitQuizAnswer(sessionId, studentId, userId, answerText));
    }

    @PostMapping("/quiz/grade")
    public R<Map<String, Object>> gradeQuiz(@Valid @RequestBody QuizGradeRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        String response = request.getResponse() != null ? request.getResponse() : "";
        int score = request.getScore() != null ? request.getScore() : 1;
        return R.ok(classroomService.gradeQuiz(request.getSessionId(), request.getStudentId(),
                request.getResult(), response, score));
    }

    /** AI即时出题：根据学科和知识点生成课堂提问，同时存入 classroom_questions 表 */
    @PostMapping("/quiz/ai-generate")
    public R<List<Map<String, Object>>> aiGenerateQuiz(@RequestBody AiQuizGenerateRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可调用");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "未找到教师信息");
        long now = System.currentTimeMillis();
        java.util.concurrent.CopyOnWriteArrayList<Long> timestamps = aiGenRateLimiter.computeIfAbsent(teacherId, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        timestamps.removeIf(t -> now - t > 60_000);
        if (timestamps.isEmpty()) {
            aiGenRateLimiter.remove(teacherId);
        }
        if (timestamps.size() >= 5) return R.error(429, "AI出题过于频繁，请稍后再试");
        timestamps.add(now);

        String subject = request.getSubject() != null ? request.getSubject() : "";
        String knowledgePoint = request.getKnowledgePoint() != null ? request.getKnowledgePoint() : "";
        int count = request.getCount() != null ? request.getCount() : 5;
        if (count < 3) count = 3;
        if (count > 8) count = 8;
        String stageHint = request.getStageHint() != null ? request.getStageHint() : "";

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contentType", "CLASSROOM_QUESTIONS");
        params.put("subject", subject);
        params.put("knowledgePoint", knowledgePoint);
        if (!stageHint.isEmpty()) params.put("stageHint", stageHint);
        params.put("candidateCount", count);
        if (request.getQuestionType() != null && !request.getQuestionType().isEmpty()) {
            params.put("questionType", request.getQuestionType());
        } else {
            params.put("questionType", "SHORT_ANSWER");
        }
        if (request.getNodeId() != null) params.put("categoryId", request.getNodeId());
        if (request.getTeachingGoal() != null && !request.getTeachingGoal().isEmpty()) {
            params.put("teachingGoal", request.getTeachingGoal());
        }
        if (request.getDifficulty() != null) {
            params.put("difficulty", request.getDifficulty());
        }

        Map<String, Object> result = aiContentGeneratorService.generateSync(teacherId, params);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.getOrDefault("questions", List.of());

        for (Map<String, Object> q : questions) {
            ClassroomQuestion cq = new ClassroomQuestion();
            cq.setTeacherId(teacherId);
            cq.setContent(q.get("questionText") instanceof String s ? s : "");
            cq.setReferenceAnswer(q.get("correctAnswer") instanceof String s ? s : "");
            cq.setQuestionType(q.get("questionType") instanceof String s ? s : "SHORT_ANSWER");
            cq.setSource("AI_SMARTSCREEN");
            cq.setFromAi(1);
            cq.setSubject(subject);
            cq.setAiCategory(q.get("category") instanceof String s ? s : "");
            cq.setIntent(q.get("intent") instanceof String s ? s : "");
            classroomService.insertAiGeneratedQuestion(cq);
        }
        return R.ok(questions);
    }

    // === 抢答 ===
    @PostMapping("/buzz/start")
    public R<Map<String, Object>> startBuzz(@Valid @RequestBody BuzzStartRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "未找到教师信息");
        int scoreReward = request.getScoreReward() != null ? request.getScoreReward() : 3;
        return R.ok(classroomService.startBuzz(request.getClassId(), teacherId, request.getQuestionText(), scoreReward));
    }

    @PostMapping("/buzz/submit")
    public R<Map<String, Object>> submitBuzz(@Valid @RequestBody BuzzSubmitRequest request) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        return R.ok(classroomService.submitBuzz(request.getSessionId(), studentId, userId));
    }

    @PostMapping("/buzz/grade")
    public R<Map<String, Object>> gradeBuzz(@Valid @RequestBody BuzzGradeRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        String response = request.getResponse() != null ? request.getResponse() : "";
        return R.ok(classroomService.gradeBuzz(request.getSessionId(), request.getStudentId(),
                request.getResult(), response));
    }

    // === 投票 ===
    @PostMapping("/poll/start")
    public R<Map<String, Object>> startPoll(@Valid @RequestBody PollStartRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "未找到教师信息");
        int durationSeconds = request.getDurationSeconds() != null ? request.getDurationSeconds() : 30;
        boolean anonymous = request.getAnonymous() == null || request.getAnonymous();
        return R.ok(classroomService.startPoll(request.getClassId(), teacherId, request.getQuestionText(),
                request.getOptions(), durationSeconds, anonymous));
    }

    @PostMapping("/poll/vote")
    public R<Map<String, Object>> submitVote(@Valid @RequestBody PollVoteRequest request) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        if (request.getOptionIndex() < 0 || request.getOptionIndex() > 9) return R.error(400, "选项索引越界");
        return R.ok(classroomService.submitVote(request.getSessionId(), studentId, userId, request.getOptionIndex()));
    }

    @PostMapping("/poll/end")
    public R<Map<String, Object>> endPoll(@Valid @RequestBody PollEndRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(classroomService.endPoll(request.getSessionId(), request.getManualCounts()));
    }

    // ═══════════ 随堂速答 (LIVE_QUIZ) ═══════════

    @PostMapping("/live-quiz/start")
    public R<Map<String, Object>> startLiveQuiz(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long teacherId = SecurityUtils.getCurrentUserId();
        Long classId = body.get("classId") instanceof Number ? ((Number) body.get("classId")).longValue() : null;
        if (classId == null) return R.error(400, "请指定班级");
        if (!canAccessClass(classId)) return R.error(403, "无权操作该班级");
        String questionText = (String) body.getOrDefault("questionText", "");
        String mode = (String) body.getOrDefault("mode", "choice");
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.get("options");
        String correctAnswer = (String) body.getOrDefault("correctAnswer", "");
        int durationSeconds = body.get("durationSeconds") instanceof Number ? ((Number) body.get("durationSeconds")).intValue() : 30;
        return R.ok(classroomService.startLiveQuiz(classId, teacherId, questionText, mode, options, correctAnswer, durationSeconds));
    }

    @PostMapping("/live-quiz/submit")
    public R<Map<String, Object>> submitLiveQuizAnswer(@RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long sessionId = body.get("sessionId") instanceof Number ? ((Number) body.get("sessionId")).longValue() : null;
        if (sessionId == null) return R.error(400, "请指定sessionId");
        String answer = (String) body.getOrDefault("answer", "");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(classroomService.submitLiveQuizAnswer(sessionId, sid, userId, answer));
    }

    @PostMapping("/live-quiz/end")
    public R<Map<String, Object>> endLiveQuiz(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long sessionId = body.get("sessionId") instanceof Number ? ((Number) body.get("sessionId")).longValue() : null;
        if (sessionId == null) return R.error(400, "请指定sessionId");
        boolean revealAnswer = body.get("revealAnswer") instanceof Boolean && (Boolean) body.get("revealAnswer");
        return R.ok(classroomService.endLiveQuiz(sessionId, revealAnswer));
    }

    @PostMapping("/live-quiz/pick")
    public R<Map<String, Object>> pickLiveQuizStudent(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long teacherId = SecurityUtils.getCurrentUserId();
        Long classId = body.get("classId") instanceof Number ? ((Number) body.get("classId")).longValue() : null;
        if (classId == null) return R.error(400, "请指定班级");
        if (!canAccessClass(classId)) return R.error(403, "无权操作该班级");
        @SuppressWarnings("unchecked")
        List<Long> excludeStudentIds = (List<Long>) body.get("excludeStudentIds");
        return R.ok(classroomService.pickLiveQuizStudent(classId, teacherId, excludeStudentIds));
    }

    // === 查询 ===
    @GetMapping("/sessions")
    public R<List<Map<String, Object>>> getSessions(@RequestParam Long classId, @RequestParam(defaultValue = "50") int limit) {
        return R.ok(classroomService.getSessions(classId, limit));
    }

    @GetMapping("/scores")
    public R<List<Map<String, Object>>> getScores(@RequestParam Long classId) {
        return R.ok(classroomService.getClassroomScores(classId));
    }

    // === 抽问题库 CRUD ===
    @GetMapping("/questions")
    public R<Map<String, Object>> getQuestions(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        return R.ok(classroomService.getQuestions(teacherId, taskId, keyword, page, pageSize));
    }

    @GetMapping("/questions/filters")
    public R<Map<String, Object>> getQuestionFilters() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        return R.ok(classroomService.getQuestionFilters(teacherId));
    }

    @PostMapping("/questions")
    public R<Map<String, Object>> addQuestion(@Valid @RequestBody ClassroomQuestionRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        return R.ok(classroomService.addQuestion(teacherId, request));
    }

    @PutMapping("/questions/{id}")
    public R<Map<String, Object>> updateQuestion(@PathVariable Long id, @RequestBody ClassroomQuestionRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(classroomService.updateQuestion(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public R<String> deleteQuestion(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        classroomService.deleteQuestion(id);
        return R.ok("已删除");
    }

    @PostMapping("/questions/batch-import")
    public R<List<Map<String, Object>>> batchImport(@RequestBody QuestionImportRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        return R.ok(classroomService.batchImportQuestions(teacherId, request.getRows()));
    }

    /** Excel文件批量导入抽问题目 — 使用Apache POI解析 */
    @PostMapping("/questions/batch-import-excel")
    public R<List<Map<String, Object>>> batchImportExcel(@RequestParam("file") MultipartFile file) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        if (file.isEmpty()) return R.error(400, "文件为空");
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            return R.error(400, "仅支持 .xlsx / .xls 格式");
        }
        try {
            List<Map<String, Object>> rows = classroomService.parseExcelRows(file);
            return R.ok(classroomService.batchImportQuestions(teacherId, rows));
        } catch (Exception e) {
            return R.error(400, "Excel解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/questions/import-from-bank")
    public R<List<Map<String, Object>>> importFromBank(@RequestBody QuestionBankImportRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        List<Long> ids = request.getQuestionIds().stream().map(Integer::longValue).toList();
        return R.ok(classroomService.importFromQuestionBank(teacherId, ids));
    }

    /** AI教学助手推荐题目（已审核通过的课堂提问类型），按教师隔离 */
    @GetMapping("/questions/ai-recommended")
    public R<List<ClassroomQuestion>> getAiRecommended(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String tag) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        return R.ok(classroomService.getAiRecommended(teacherId, subject, tag));
    }

    /** txt 文件批量导入抽问题目（每行一题，关联到知识节点任务） */
    @PostMapping("/questions/batch-import-txt")
    public R<List<Map<String, Object>>> batchImportTxt(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long taskId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) teacherId = userId;
        if (file.isEmpty()) return R.error(400, "文件为空");
        String name = file.getOriginalFilename();
        if (name == null || !name.endsWith(".txt")) {
            return R.error(400, "仅支持 .txt 格式");
        }
        try {
            return R.ok(classroomService.batchImportTxt(teacherId, file, taskId));
        } catch (Exception e) {
            return R.error(400, "txt解析失败: " + e.getMessage());
        }
    }

    // === 缺席学生管理 ===

    @PostMapping("/students/absent")
    public R<String> markAbsent(@RequestBody AbsentStudentsRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        List<Long> studentIds = request.getStudentIds() != null
            ? request.getStudentIds().stream().map(Integer::longValue).toList() : List.of();
        classroomService.markAbsentStudents(request.getClassId(), studentIds);
        return R.ok("已标记 " + studentIds.size() + " 人缺席");
    }

    @GetMapping("/students/absent")
    public R<List<Long>> getAbsent(@RequestParam Long classId) {
        return R.ok(classroomService.getAbsentStudents(classId));
    }

    @DeleteMapping("/students/absent")
    public R<String> clearAbsent(@RequestParam Long classId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(classId)) return R.error(403, "无权操作该班级");
        classroomService.clearAbsentStudents(classId);
        return R.ok("已清除");
    }

    @DeleteMapping("/students/absent/{studentId}")
    public R<String> unmarkAbsent(@RequestParam Long classId, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(classId)) return R.error(403, "无权操作该班级");
        classroomService.unmarkAbsentStudent(classId, studentId);
        return R.ok("已取消缺席标记");
    }

    /** 课堂互动数据分析 */
    @GetMapping("/analytics")
    public R<Map<String, Object>> getClassroomAnalytics(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "30d") String dateRange) {
        if (!canAccessClass(classId)) return R.error(403, "无权访问该班级数据");
        return R.ok(classroomService.getClassroomAnalytics(classId, dateRange));
    }

    // === 课堂任务联动 ===

    /** 获取班级当前进行中的课堂任务 */
    @GetMapping("/tasks/active")
    public R<List<Task>> getActiveClassTasks(@RequestParam Long classId) {
        if (!canAccessClass(classId)) return R.error(403, "无权访问该班级数据");
        List<Task> tasks = taskService.getActiveClassTasks(classId);
        return R.ok(tasks);
    }

    /** 教师启动课堂任务，SSE广播给全班学生 */
    @PostMapping("/tasks/{taskId}/actions/start")
    public R<String> startClassTask(@PathVariable Long taskId, @RequestParam Long classId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        if (!canAccessClass(classId)) return R.error(403, "无权操作该班级");
        Task task = taskService.getById(taskId);
        if (task == null) return R.error(404, "任务不存在");
        classroomService.broadcastTaskStarted(classId, taskId, task.getTitle());
        return R.ok("已启动，SSE广播已发送");
    }

    // === 抽问池管理 ===

    @PostMapping("/quiz/remove-student")
    public R<?> removeFromQuizPool(@Valid @RequestBody RemoveFromQuizPoolRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        classroomService.removeFromQuizPool(request.getClassId(), request.getStudentId());
        return R.ok("已从抽问池移除");
    }

    /** 重置抽问池：清空最近2小时抽问参与记录，开始新一轮 */
    @PostMapping("/quiz/reset-pool")
    public R<?> resetQuizPool(@Valid @RequestBody ResetQuizPoolRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (!canAccessClass(request.getClassId())) return R.error(403, "无权操作该班级");
        classroomService.resetQuizPool(request.getClassId());
        return R.ok("抽问池已重置，新一轮开始");
    }

    /** 班级SSE在线人数 */
    @GetMapping("/class/{classId}/online-count")
    public R<Map<String, Object>> onlineCount(@PathVariable Long classId) {
        int count = classroomService.getClassroomOnlineCount(classId);
        return R.ok(Map.of("onlineCount", count));
    }

    /** 短轮询降级端点：学生端SSE失败时每2秒轮询，返回当前活动状态 */
    @GetMapping("/class/{classId}/poll-state")
    public R<Map<String, Object>> pollState(@PathVariable Long classId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        // 学生端校验班级归属
        if (!SecurityUtils.isTeacherOrAdmin()) {
            Student student = studentService.getStudentByUserId(userId);
            if (student == null || !classId.equals(student.getClassId())) {
                return R.error(403, "无权访问该班级");
            }
        }
        return R.ok(classroomService.getActiveSessionState(classId));
    }

    /** 获取课堂任务实时提交进度 */
    @GetMapping("/tasks/{taskId}/progress")
    public R<Map<String, Object>> getTaskProgress(@PathVariable Long taskId, @RequestParam Long classId) {
        if (!canAccessClass(classId)) return R.error(403, "无权访问该班级数据");
        long total = studentService.countStudentsByClassId(classId);
        long submitted = taskSubmissionService.countSubmissionsByTaskAndStatus(taskId, java.util.List.of("SUBMITTED", "GRADED"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", total);
        result.put("submittedCount", submitted);
        return R.ok(result);
    }

    @PostMapping("/class/{classId}/quick-review/start")
    public R<?> startQuickReview(@PathVariable Long classId, @RequestBody QuickReviewStartRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        return R.ok(cardRecommendationService.startQuickReview(classId, request.getSubjectId(), request.getNodeId(), limit));
    }

    @PostMapping("/quick-review/{sessionId}/record")
    public R<?> recordQuickReview(@PathVariable String sessionId, @Valid @RequestBody QuickReviewRecordRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        int cardIndex = request.getCardIndex() != null ? request.getCardIndex() : 0;
        boolean correct = Boolean.TRUE.equals(request.getCorrect());
        cardRecommendationService.recordQuickReview(sessionId, request.getStudentId(), cardIndex, correct);
        return R.ok(null, correct ? "答对 +1分" : "已记录");
    }
}
