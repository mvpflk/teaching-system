package com.school.teaching.precision;

import com.school.teaching.common.R;
import com.school.teaching.precision.impl.PrecisionDiagnosisService;
import com.school.teaching.precision.impl.PrecisionReportService;
import com.school.teaching.precision.impl.PrecisionStudyService;
import com.school.teaching.precision.impl.PrecisionTeacherService;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/precision")
public class PrecisionController {

    @Autowired private PrecisionDiagnosisService diagnosisService;
    @Autowired private PrecisionTeacherService teacherService;
    @Autowired private PrecisionStudyService studyService;
    @Autowired private PrecisionReportService reportService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private com.school.teaching.service.SystemService systemService;
    @Autowired(required = false) private com.school.teaching.precision.impl.PrecisionEnglishServiceImpl englishService;

    // AI 端点速率限制：studentId → 最近调用时间戳列表（60秒内最多10次）
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Long>> aiRateLimiter = new ConcurrentHashMap<>();
    private static final int AI_RATE_LIMIT = 10; // 60秒内最多10次
    private static final long AI_RATE_WINDOW_MS = 60_000L; // 60秒窗口
    private static final long AI_RATE_CLEANUP_MS = 5 * 60_000L; // 5分钟清理一次过期条目
    private volatile long lastAiRateCleanup = System.currentTimeMillis();

    /** 从 dict_subject 动态获取职高学科列表（已下沉到 SystemService） */
    private List<String> getVocationalSubjects() {
        try {
            List<String> subjects = systemService.getVocationalSubjects();
            if (subjects != null && !subjects.isEmpty()) return subjects;
        } catch (Exception e) {
            log.warn("从数据库加载职高学科列表失败，使用默认列表", e);
        }
        return java.util.List.of("数学[职高]", "英语[职高]", "信息技术应用基础[职高]");
    }

    private <T> R<T> checkEnglishEnabled() {
        R<T> parent = checkEnabled(); if (parent != null) return parent;
        if (!systemService.getBooleanConfig("feature.english_remedial", true)) {
            @SuppressWarnings("unchecked") R<T> d = R.error(403, "英语模块暂未开放");
            return d;
        }
        return null;
    }

    private <T> R<T> checkEnabled() {
        if (!systemService.getBooleanConfig("feature.remedial_enabled", true)) {
            @SuppressWarnings("unchecked") R<T> disabled = R.error(403, "偏科提分模块暂未开放");
            return disabled;
        }
        if (!systemService.isRemedialEnabledForCurrentUser()) {
            @SuppressWarnings("unchecked") R<T> disabled = R.error(403, "偏科提分模块暂未对您所在班级开放");
            return disabled;
        }
        return null;
    }

    /** AI 端点速率限制：60秒内最多10次调用 */
    private <T> R<T> checkAiRateLimit(Long studentId) {
        long now = System.currentTimeMillis();
        // 定期清理过期条目，防止内存泄漏
        if (now - lastAiRateCleanup > AI_RATE_CLEANUP_MS) {
            aiRateLimiter.entrySet().removeIf(e -> {
                e.getValue().removeIf(t -> now - t > AI_RATE_WINDOW_MS);
                return e.getValue().isEmpty();
            });
            lastAiRateCleanup = now;
        }
        CopyOnWriteArrayList<Long> timestamps = aiRateLimiter.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>());
        timestamps.removeIf(t -> now - t > AI_RATE_WINDOW_MS);
        if (timestamps.size() >= AI_RATE_LIMIT) {
            return R.error(429, "AI调用过于频繁，请稍后再试");
        }
        timestamps.add(now);
        return null;
    }

    // ═══════════════ 通用 ═══════════════

    @GetMapping("/access")
    public R<Map<String, Boolean>> checkAccess() {
        boolean canAccess = systemService.getBooleanConfig("feature.remedial_enabled", true)
                && systemService.isRemedialEnabledForCurrentUser();
        return R.ok(Map.of("canAccess", canAccess));
    }

    // ═══════════════ 学生端 ═══════════════

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(diagnosisService.getDashboard(sid));
    }

    @GetMapping("/diagnose")
    public R<Map<String, Object>> getDiagnosis(@RequestParam @NotBlank String subject) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(diagnosisService.getDiagnosis(sid, subject));
    }

    @PostMapping("/diagnose/submit")
    public R<Map<String, Object>> submitDiagnosis(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        R<Map<String, Object>> rateLimited = checkAiRateLimit(sid); if (rateLimited != null) return rateLimited;
        String subject = String.valueOf(body.get("subject"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return R.ok(diagnosisService.submitDiagnosis(sid, subject, answers));
    }

    /**
     * 逐题判分 — 诊断即时反馈
     * 学生每答一题立即调用，返回对错+正确答案+解析
     */
    @PostMapping("/diagnose/answer")
    public R<Map<String, Object>> gradeOneAnswer(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long questionId = body.get("questionId") instanceof Number n ? n.longValue() : null;
        String answer = body.get("answer") instanceof String s ? s : "";
        String subject = body.get("subject") instanceof String s ? s : "";
        String questionType = body.get("questionType") instanceof String s ? s : "";
        if (questionId == null) return R.error(400, "缺少题目ID");
        return R.ok(diagnosisService.gradeOneAnswer(sid, questionId, answer, subject, questionType));
    }

    @GetMapping(value = "/weekly-pack", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String getWeeklyPack(@RequestParam String subject,
                                 @RequestParam(defaultValue = "1") @Min(1) @Max(52) int week) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return "<html><body><p>" + disabled.getMessage() + "</p></body></html>";
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return "<html><body><p>未找到学生信息</p></body></html>";
        try {
            return studyService.getWeeklyPackHtml(sid, subject, week);
        } catch (com.school.teaching.exception.BusinessException e) {
            return "<html><body><h2>⚠ " + e.getMessage() + "</h2></body></html>";
        } catch (Exception e) {
            log.error("生成学习包失败 subject={} week={} studentId={}", subject, week, sid, e);
            return "<html><body><h2>⚠ 生成学习包失败</h2><p>请稍后重试，或联系老师获取打印版。</p></body></html>";
        }
    }

    /** 获取学习包结构化题目（在线答题模式，无时间门控） */
    @GetMapping("/pack-questions")
    public R<List<Map<String, Object>>> getPackQuestions(@RequestParam String subject) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(studyService.getPackQuestions(sid, subject));
    }

    @GetMapping("/online-test")
    public R<Map<String, Object>> getOnlineTest(@RequestParam String subject) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(studyService.getOnlineTest(sid, subject));
    }

    @PostMapping("/online-test/submit")
    public R<Map<String, Object>> submitOnlineTest(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(studyService.submitOnlineTest(sid, body));
    }

    @GetMapping("/report")
    public R<Map<String, Object>> getReport(@RequestParam String subject) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(reportService.getReport(sid, subject));
    }

    @GetMapping("/practice-questions")
    public R<List<Map<String, Object>>> getPracticeQuestions(
            @RequestParam @Min(1) Long nodeId,
            @RequestParam(required = false) String subject) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(reportService.getPracticeQuestions(sid, nodeId, subject));
    }

    @GetMapping("/syllabus-map")
    public R<List<Map<String, Object>>> getSyllabusMap(@RequestParam String subject) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(reportService.getSyllabusMap(sid, subject));
    }

    @PostMapping("/ai-qa")
    public R<Map<String, Object>> aiQa(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        R<Map<String, Object>> rateLimited = checkAiRateLimit(sid); if (rateLimited != null) return rateLimited;
        String question = String.valueOf(body.getOrDefault("question", ""));
        if (question.isBlank() || question.length() > 500) return R.error(400, "问题长度需在1-500字之间");
        return R.ok(reportService.aiQa(sid, question));
    }

    // ═══════════════ 教师端 ═══════════════

    @GetMapping("/teacher/overview")
    public R<Map<String, Object>> teacherOverview() {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(teacherService.teacherOverview(userId));
    }

    @GetMapping("/teacher/students")
    public R<List<Map<String, Object>>> teacherStudents(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String subject) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(teacherService.teacherStudents(userId, groupId, subject));
    }

    @PostMapping("/teacher/remind-all")
    public R<Integer> remindAll(@RequestBody Map<String, String> body) {
        R<Integer> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        String subject = body.getOrDefault("subject", "英语[职高]");
        return R.ok(teacherService.remindAll(userId, subject));
    }

    @PostMapping("/teacher/compose")
    public R<Map<String, Object>> composeRemedial(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        Long groupId = body.get("groupId") instanceof Number n ? n.longValue() : null;
        Long classId = body.get("classId") instanceof Number n ? n.longValue() : null;
        String subject = String.valueOf(body.getOrDefault("subject", "数学[职高]"));
        // 校验班级归属
        if (classId != null) teacherService.assertTeacherOwnsClass(userId, classId);
        return R.ok(teacherService.composeRemedialTask(userId, groupId, classId, subject));
    }

    @GetMapping("/teacher/weak-top")
    public R<List<Map<String, Object>>> teacherWeakTop(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int topN) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(teacherService.teacherWeakTop(userId,
            subject != null ? subject : "数学[职高]", topN));
    }

    @PostMapping("/teacher/remind-student")
    public R<Boolean> remindStudent(@RequestBody Map<String, Object> body) {
        R<Boolean> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        Long studentId = body.get("studentId") instanceof Number n ? n.longValue() : null;
        if (studentId == null) return R.error(400, "缺少学生ID");
        teacherService.assertTeacherOwnsStudent(userId, studentId);
        String subject = String.valueOf(body.getOrDefault("subject", "英语[职高]"));
        return R.ok(teacherService.remindStudent(userId, studentId, subject));
    }

    @GetMapping("/teacher/class-weaknesses")
    public R<Map<String, Object>> getClassWeaknesses(@RequestParam Long classId) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        teacherService.assertTeacherOwnsClass(SecurityUtils.getCurrentUserId(), classId);
        return R.ok(teacherService.getClassWeaknesses(classId));
    }

    /**
     * 批量查询偏科提分状态 — 质量分析页通过 studentId+kpId 列表批量查 precision_progress。
     * POST /precision/teacher/student-kp-status
     * Body: { "pairs": [ {"studentId":1, "kpId":10}, ... ] }
     */
    @PostMapping("/teacher/student-kp-status")
    public R<List<Map<String, Object>>> batchKpStatus(@RequestBody Map<String, Object> body) {
        R<List<Map<String, Object>>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) body.getOrDefault("pairs", List.of());
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> pair : pairs) {
            Long studentId = pair.get("studentId") instanceof Number n ? n.longValue() : null;
            Long kpId = pair.get("kpId") instanceof Number n ? n.longValue() : null;
            if (studentId == null || kpId == null) continue;
            teacherService.assertTeacherOwnsStudent(userId, studentId);
            Map<String, Object> status = reportService.getStudentKpStatus(studentId, kpId);
            status.put("studentId", studentId);
            status.put("kpId", kpId);
            result.add(status);
        }
        return R.ok(result);
    }

    /**
     * 质量分析页一键创建偏科提分学习包 — 根据 studentId+kpId 生成精准练习题。
     * POST /precision/teacher/ensure-from-quality
     * Body: { "studentId":1, "kpId":10, "subject":"信息技术应用基础" }
     */
    @PostMapping("/teacher/ensure-from-quality")
    public R<Map<String, Object>> ensureFromQuality(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        Long studentId = body.get("studentId") instanceof Number n ? n.longValue() : null;
        Long kpId = body.get("kpId") instanceof Number n ? n.longValue() : null;
        String subject = body.get("subject") instanceof String s ? s : "";
        if (studentId == null || kpId == null) return R.error(400, "缺少 studentId 或 kpId");
        teacherService.assertTeacherOwnsStudent(userId, studentId);
        return R.ok(reportService.ensureFromQuality(studentId, kpId, subject));
    }

    /**
     * 拍照上传数学解答 — Vision OCR 识别并存储。
     * POST /precision/upload-answer
     * multipart/form-data: { file, questionId, questionType }
     * 返回: { attachmentPath, ocrText, confidence }
     */
    @PostMapping(value = "/upload-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> uploadAnswer(
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionId") Long questionId,
            @RequestParam(value = "questionType", defaultValue = "CALCULATION") String questionType) {
        R<Map<String, Object>> disabled = checkEnabled(); if (disabled != null) return disabled;
        if (file == null || file.isEmpty()) return R.error(400, "请选择要上传的文件");
        if (file.getSize() > 5 * 1024 * 1024) return R.error(400, "文件大小不能超过5MB");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(reportService.uploadAnswerPhoto(sid, questionId, questionType, file));
    }

    // ═══════════════ 英语模块 ═══════════════

    @GetMapping("/english/dashboard")
    public R<Map<String, Object>> englishDashboard() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getDashboard(sid));
    }

    @GetMapping("/english/daily-task")
    public R<Map<String, Object>> englishDailyTask() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Map<String, Object> dash = englishService.getDashboard(sid);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = (Map<String, Object>) dash.get("dailyTask");
        return R.ok(task);
    }

    @PostMapping("/english/drill/submit")
    public R<Map<String, Object>> submitDrill(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long qid = body.get("questionId") instanceof Number n ? n.longValue() : null;
        String answer = String.valueOf(body.getOrDefault("answer", ""));
        int hintLevel = body.get("hintLevel") instanceof Number n ? n.intValue() : 0;
        String qType = String.valueOf(body.getOrDefault("questionType", ""));
        String subject = String.valueOf(body.getOrDefault("subject", "英语[职高]"));
        String word = body.get("word") instanceof String s ? s : null;
        String direction = body.get("direction") instanceof String s ? s : null;
        return R.ok(englishService.submitDrillAnswer(sid, qid, answer, hintLevel, qType, subject, word, direction));
    }

    @PostMapping("/english/drill/complete")
    public R<Map<String, Object>> completeDrill(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        int groupSeq = body.get("groupSeq") instanceof Number n ? n.intValue() : 1;
        int elapsedSeconds = body.get("elapsedSeconds") instanceof Number n ? n.intValue() : 0;
        return R.ok(englishService.completeDrill(sid, answers, groupSeq, elapsedSeconds));
    }

    @PostMapping("/english/freeze-card/use")
    public R<Map<String, Object>> useFreezeCard() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.useFreezeCard(sid));
    }

    @GetMapping("/english/freeze-card/status")
    public R<Map<String, Object>> freezeCardStatus() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.freezeCardStatus(sid));
    }

    @GetMapping("/english/grammar-tree")
    public R<List<Map<String, Object>>> grammarTree() {
        R<List<Map<String, Object>>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getGrammarTree(sid));
    }

    @GetMapping("/english/grammar-practice")
    public R<List<Map<String, Object>>> grammarPractice(@RequestParam Long nodeId) {
        R<List<Map<String, Object>>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getGrammarPractice(sid, nodeId));
    }

    @GetMapping("/english/reading")
    public R<Map<String, Object>> reading() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getReading(sid));
    }

    @PostMapping("/english/stage-test")
    public R<List<Map<String, Object>>> stageTest(@RequestBody Map<String, Object> body) {
        R<List<Map<String, Object>>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        int stage = body.get("stage") instanceof Number n ? n.intValue() : 1;
        String testType = String.valueOf(body.getOrDefault("testType", "vocab"));
        return R.ok(englishService.getStageTest(sid, stage, testType));
    }

    @GetMapping("/english/ranking")
    public R<List<Map<String, Object>>> englishRanking() {
        R<List<Map<String, Object>>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getClassRanking(sid));
    }

    @GetMapping("/english/vocab-book")
    public R<Map<String, Object>> vocabBook() {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(englishService.getVocabBook(sid));
    }

    @GetMapping("/teacher/english/students")
    public R<Map<String, Object>> teacherEnglishStudents(@RequestParam Long classId) {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        teacherService.assertTeacherOwnsClass(SecurityUtils.getCurrentUserId(), classId);
        return R.ok(englishService.teacherEnglishStudents(classId));
    }

    @GetMapping("/teacher/english/student/{studentId}")
    public R<Map<String, Object>> teacherEnglishDetail(@PathVariable Long studentId) {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        teacherService.assertTeacherOwnsStudent(SecurityUtils.getCurrentUserId(), studentId);
        return R.ok(englishService.getDashboard(studentId));
    }

    @PostMapping("/teacher/english/remind")
    public R<Integer> remindEnglish(@RequestBody Map<String, Object> body) {
        R<Integer> d = checkEnglishEnabled(); if (d != null) return d;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long classId = body.get("classId") instanceof Number n ? n.longValue() : null;
        if (classId != null) teacherService.assertTeacherOwnsClass(SecurityUtils.getCurrentUserId(), classId);
        String msg = String.valueOf(body.getOrDefault("message", ""));
        return R.ok(englishService.remindClass(classId, msg));
    }

    @GetMapping("/teacher/english/report")
    public R<Map<String, Object>> teacherEnglishReport(@RequestParam Long classId) {
        R<Map<String, Object>> d = checkEnglishEnabled(); if (d != null) return d;
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        teacherService.assertTeacherOwnsClass(SecurityUtils.getCurrentUserId(), classId);
        return R.ok(englishService.teacherEnglishReport(classId));
    }

    // ═══════════════ 解答题教师审核 ═══════════════

    /** 教师端：查询待审核的解答题列表 */
    @GetMapping("/teacher/calc-reviews")
    public R<List<Map<String, Object>>> pendingCalcReviews(
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int status) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        if (classId != null) teacherService.assertTeacherOwnsClass(userId, classId);
        return R.ok(teacherService.getPendingCalcReviews(userId, classId, status));
    }

    /** 教师端：对解答题进行评分 */
    @PostMapping("/teacher/grade-calc-review")
    public R<Map<String, Object>> gradeCalcReview(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long reviewId = body.get("reviewId") instanceof Number n ? n.longValue() : null;
        Integer teacherScore = body.get("teacherScore") instanceof Number n ? n.intValue() : null;
        String teacherComment = String.valueOf(body.getOrDefault("teacherComment", ""));
        if (reviewId == null || teacherScore == null) return R.error(400, "缺少 reviewId 或 teacherScore");
        return R.ok(teacherService.gradeCalcReview(reviewId, teacherScore, teacherComment));
    }
}
