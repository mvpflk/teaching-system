package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.TypingService;
import com.school.teaching.sse.SseConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/typing")
public class TypingController {

    @Autowired private TypingService typingService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private SseConnectionManager sseConnectionManager;

    // #14 修复：练习会话跟踪，避免 startPractice 无状态
    private final ConcurrentHashMap<String, Map<String, Object>> practiceSessions = new ConcurrentHashMap<>();

    // ═══════════ 权限与配置 ═══════════

    @GetMapping("/check-permission")
    public R<Map<String, Object>> checkPermission() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.ok(Map.of("allowed", false, "reason", "未找到学生信息"));
        boolean allowed = typingService.checkStudentPermission(sid);
        return R.ok(Map.of("allowed", allowed,
            "reason", allowed ? "OK" : "你的专业暂未开放打字功能"));
    }

    @GetMapping("/settings/majors")
    public R<List<Integer>> getTypingMajors() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.getTypingAllowedMajors());
    }

    @PutMapping("/settings/majors")
    public R<?> updateTypingMajors(@RequestBody Map<String, List<Integer>> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        typingService.setTypingAllowedMajors(body.get("majorIds"));
        return R.ok("已更新");
    }

    // ═══════════ 文本管理（教师/管理员） ═══════════

    @GetMapping("/texts")
    public R<Map<String, Object>> getTexts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String category) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.getTexts(page, size, type, keyword, language, difficulty, category));
    }

    @PostMapping("/texts")
    public R<TypingText> addText(@RequestBody TypingText text) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        text.setCreatedBy(SecurityUtils.getCurrentUserId());
        return R.ok(typingService.addText(text));
    }

    @PutMapping("/texts/{id}")
    public R<TypingText> updateText(@PathVariable Long id, @RequestBody TypingText text) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.updateText(id, text));
    }

    @DeleteMapping("/texts/{id}")
    public R<?> deleteText(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        typingService.deleteText(id);
        return R.ok("已删除");
    }

    // ═══════════ 竞赛管理 ═══════════

    @GetMapping("/competitions")
    public R<Map<String, Object>> getCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.getCompetitions(page, size, status));
    }

    @PostMapping("/competitions")
    public R<TypingCompetition> createCompetition(@RequestBody TypingCompetition comp) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        comp.setCreatedBy(SecurityUtils.getCurrentUserId());
        return R.ok(typingService.createCompetition(comp));
    }

    @PutMapping("/competitions/{id}/start")
    public R<?> startCompetition(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        typingService.startCompetition(id);
        return R.ok("竞赛已开始");
    }

    @PutMapping("/competitions/{id}/finish")
    public R<?> finishCompetition(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        typingService.finishCompetition(id);
        return R.ok("竞赛已结束");
    }

    @DeleteMapping("/competitions/{id}")
    public R<?> deleteCompetition(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        typingService.deleteCompetition(id);
        return R.ok("已删除");
    }

    @GetMapping("/competitions/current")
    public R<TypingCompetition> getCurrentCompetition() {
        Long sid = studentResolver.resolveCurrentStudentId();
        return R.ok(typingService.getCurrentCompetition(sid));
    }

    @GetMapping("/competitions/{id}/ranking")
    public R<List<Map<String, Object>>> getRanking(@PathVariable Long id) {
        return R.ok(typingService.getRanking(id));
    }

    @GetMapping("/competitions/{id}/dashboard")
    public R<Map<String, Object>> getDashboard(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.getDashboard(id));
    }

    @PostMapping("/competitions/{id}/submit")
    public R<TypingCompetitionResult> submitResult(
            @PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(typingService.submitResult(id, sid, data));
    }

    @GetMapping("/competitions/{id}/export")
    public R<List<Map<String, Object>>> exportResults(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.exportResults(id));
    }

    // ═══════════ 学生打字过程 ═══════════

    @PostMapping("/session/progress")
    public R<?> reportProgress(@RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long competitionId = body.get("competitionId") != null
            ? Long.valueOf(body.get("competitionId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) body.get("progress");
        if (competitionId == null || progress == null) return R.error("参数错误");
        typingService.reportProgress(competitionId, sid, progress);
        return R.ok("已上报");
    }

    @GetMapping("/practice/text")
    public R<TypingText> getPracticeText(@RequestParam(required = false) Long textId,
                                         @RequestParam(required = false) Integer difficulty,
                                         @RequestParam(required = false) String language) {
        return R.ok(typingService.getRandomPracticeText(textId, difficulty, language));
    }

    @PostMapping("/records")
    public R<?> saveRecord(@RequestBody TypingRecord record) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        record.setStudentId(sid);
        // #15 修复：委托给统一的服务方法，避免重复校验逻辑
        typingService.savePracticeRecord(record);
        return R.ok("已保存");
    }

    // ── 自由练习生命周期 ──

    @PostMapping("/practice/start")
    public R<Map<String, Object>> startPractice(@RequestBody(required = false) Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long textId = null;
        if (body != null && body.get("textId") != null) {
            try { textId = Long.valueOf(body.get("textId").toString()); } catch (NumberFormatException e) { /* ignore */ }
        }
        String sessionId = java.util.UUID.randomUUID().toString().substring(0, 8);
        // #14 修复：记录练习会话状态
        Map<String, Object> session = new java.util.LinkedHashMap<>();
        session.put("studentId", sid);
        session.put("textId", textId);
        session.put("startedAt", java.time.LocalDateTime.now());
        practiceSessions.put(sessionId, session);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("startedAt", session.get("startedAt").toString());
        result.put("textId", textId);
        return R.ok(result, "练习已开始");
    }

    @PostMapping("/practice/finish")
    public R<?> finishPractice(@RequestBody TypingRecord record) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        record.setStudentId(sid);
        // #15 修复：委托给统一的服务方法，避免重复校验逻辑
        int exp = typingService.savePracticeRecord(record);
        return R.ok(Map.of("expEarned", exp), "练习完成");
    }

    // #14 修复：定时清理超过2小时的过期练习会话
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 600_000)
    public void cleanStalePracticeSessions() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(2);
        practiceSessions.entrySet().removeIf(e -> {
            Object startedAt = e.getValue().get("startedAt");
            return startedAt instanceof java.time.LocalDateTime && ((java.time.LocalDateTime) startedAt).isBefore(cutoff);
        });
    }

    // ═══════════ 学生历史/游戏化 ═══════════

    @GetMapping("/student/history")
    public R<List<TypingRecord>> getStudentHistory() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(typingService.getStudentHistory(sid));
    }

    @GetMapping("/student/wrong-words")
    public R<List<Map<String, Object>>> getWrongWords() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(typingService.getWrongWords(sid));
    }

    @GetMapping("/student/levels")
    public R<Map<String, Object>> getStudentLevels() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(typingService.getStudentLevels(sid));
    }

    @PostMapping("/student/exp")
    public R<?> addExp(@RequestBody Map<String, Integer> body) {
        // 仅管理员可用作补偿工具，学生经验统一由 saveRecord/submitResult 内部计算
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Long sid = body.get("studentId") != null ? body.get("studentId").longValue() : null;
        if (sid == null) return R.error(400, "请指定 studentId");
        int exp = body.getOrDefault("exp", 10);
        if (exp < 1 || exp > 100) return R.error(400, "经验值范围 1-100");
        typingService.addExp(sid, exp);
        return R.ok("经验已增加");
    }

    // ═══════════ SSE 实时推送 ═══════════

    // ═══════════ 速度趋势 ═══════════

    @GetMapping("/statistics")
    public R<List<Map<String, Object>>> getStudentSpeedTrend(
            @RequestParam(defaultValue = "20") int limit) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(typingService.getStudentSpeedTrend(sid, limit));
    }

    // ═══════════ 学生素材库 ═══════════

    @GetMapping("/student/texts")
    public R<Map<String, Object>> getStudentTexts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        // 学生无需权限检查，仅返回practice类型文本
        return R.ok(typingService.getTexts(page, size, "practice", keyword, language, difficulty, category));
    }

    @GetMapping("/student/texts/categories")
    public R<List<String>> getPracticeCategories() {
        return R.ok(typingService.getPracticeCategories());
    }

    // ═══════════ SSE 实时推送 ═══════════

    @GetMapping("/announcements/subscribe")
    public SseEmitter subscribeAnnouncements() {
        Long userId = SecurityUtils.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        sseConnectionManager.subscribeTypingAnnouncements(userId, emitter);
        return emitter;
    }

    @GetMapping("/competitions/{id}/subscribe")
    public SseEmitter subscribeCompetition(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        sseConnectionManager.subscribeCompetition(id, userId, emitter);
        return emitter;
    }

    @GetMapping("/competitions/{id}/replay/{studentId}")
    public R<Map<String, Object>> getCompetitionReplay(@PathVariable Long id, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(typingService.getCompetitionReplay(id, studentId));
    }
}
