package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.CheckpointConfig;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.CheckpointService;
import com.school.teaching.service.SystemService;
import com.school.teaching.service.impl.CheckpointContentHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/checkpoint")
public class CheckpointController {

    @Autowired private CheckpointService checkpointService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private SystemService systemService;

    private <T> R<T> checkEnabled() {
        if (!systemService.getBooleanConfig("feature.checkpoint_enabled", false)) {
            @SuppressWarnings("unchecked") R<T> d = R.error(410, "该功能暂未开放");
            return d;
        }
        if (!checkpointService.isStudentInWhitelist(SecurityUtils.getCurrentUserId())) {
            @SuppressWarnings("unchecked") R<T> d = R.error(403, "你的班级暂未开启闯关学习功能");
            return d;
        }
        return null;
    }

    // ═══════════════ 学生端 ═══════════════

    @GetMapping("/subjects")
    public R<List<Map<String, Object>>> listSubjects() {
        R<List<Map<String, Object>>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(checkpointService.listSubjects(sid));
    }

    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam Long subjectId) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(checkpointService.getOverview(sid, subjectId));
    }

    @GetMapping("/{configId}/start")
    public R<Map<String, Object>> start(@PathVariable Long configId) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(checkpointService.startCheckpoint(sid, configId));
    }

    @PostMapping("/{configId}/keywords/verify")
    public R<Map<String, Object>> verifyKeywords(@PathVariable Long configId, @RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return R.ok(checkpointService.verifyKeywords(sid, configId, answers));
    }

    @PostMapping("/{configId}/keywords/skip")
    public R<Map<String, Object>> skipKeyword(@PathVariable Long configId, @RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        int keywordIndex = ((Number) body.get("keywordIndex")).intValue();
        return R.ok(checkpointService.skipKeyword(sid, configId, keywordIndex));
    }

    @PostMapping("/{configId}/submit")
    public R<Map<String, Object>> submit(@PathVariable Long configId, @RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(checkpointService.submitCheckpoint(sid, configId, body));
    }

    // ═══════════════ Boss战 ═══════════════

    @PostMapping("/boss/start")
    public R<Map<String, Object>> startBoss(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        return R.ok(checkpointService.startBoss(sid, configId));
    }

    @PostMapping("/boss/submit")
    public R<Map<String, Object>> submitBoss(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return R.ok(checkpointService.submitBoss(sid, configId, answers));
    }

    @PostMapping("/boss/retry")
    public R<Map<String, Object>> retryBoss(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        return R.ok(checkpointService.retryBoss(sid, configId));
    }

    // ═══════════════ 混合战 ═══════════════

    @PostMapping("/mixed/start")
    public R<Map<String, Object>> startMixed(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        return R.ok(checkpointService.startMixed(sid, configId));
    }

    @PostMapping("/mixed/submit")
    public R<Map<String, Object>> submitMixed(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return R.ok(checkpointService.submitMixed(sid, configId, answers));
    }

    @PostMapping("/mixed/retry")
    public R<Map<String, Object>> retryMixed(@RequestBody Map<String, Object> body) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Long configId = Long.valueOf(body.get("configId").toString());
        return R.ok(checkpointService.retryMixed(sid, configId));
    }

    // ═══════════════ 记忆卡 ═══════════════

    @GetMapping("/memory-cards")
    public R<List<Map<String, Object>>> listMemoryCards(@RequestParam(required = false) Long subjectId) {
        R<List<Map<String, Object>>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(checkpointService.listMemoryCards(sid, subjectId != null ? subjectId : 0L));
    }

    @GetMapping("/memory-cards/{cardId}")
    public R<Map<String, Object>> getMemoryCard(@PathVariable Long cardId) {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Map<String, Object> card = checkpointService.getMemoryCard(cardId);
        // 权限验证：只能查看自己的记忆卡
        Object cardStudentId = card.get("studentId");
        if (cardStudentId == null || !sid.equals(Long.valueOf(cardStudentId.toString()))) {
            return R.error(403, "无权访问此记忆卡");
        }
        return R.ok(card);
    }

    @PostMapping("/memory-cards/{cardId}/review")
    public R<?> reviewMemoryCard(@PathVariable Long cardId) {
        R<?> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        // 权限验证：只能复习自己的记忆卡
        Map<String, Object> card = checkpointService.getMemoryCard(cardId);
        Object cardStudentId = card.get("studentId");
        if (cardStudentId == null || !sid.equals(Long.valueOf(cardStudentId.toString()))) {
            return R.error(403, "无权操作此记忆卡");
        }
        checkpointService.reviewMemoryCard(cardId);
        return R.ok("已记录复习");
    }

    @GetMapping("/memory-cards/unreviewed-count")
    public R<Map<String, Object>> getUnreviewedCount() {
        R<Map<String, Object>> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        int count = checkpointService.getUnreviewedCount(sid);
        return R.ok(Map.of("count", count));
    }

    // ═══════════════ 教师管理端 ═══════════════

    @GetMapping("/admin/list")
    public R<Map<String, Object>> adminList(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(checkpointService.adminList(subjectId, reviewStatus, page, size));
    }

    @PutMapping("/admin/{configId}")
    public R<?> adminUpdate(@PathVariable Long configId, @RequestBody CheckpointConfig config) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        checkpointService.adminUpdate(configId, config);

        List<String> missingSections = CheckpointContentHelper.validateContentTemplate(
            config.getKeyPointsJson());
        if (!missingSections.isEmpty()) {
            log.warn("关卡 {} 备课内容模板不完整，缺失：{}", configId, String.join("、", missingSections));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("updated", true);
            result.put("templateWarning", "备课内容模板不完整，缺失：" + String.join("、", missingSections));
            return R.ok(result);
        }
        return R.ok("已更新");
    }

    @PostMapping("/admin/{configId}/review")
    public R<?> adminReview(@PathVariable Long configId, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        boolean approved = "approve".equalsIgnoreCase(String.valueOf(body.getOrDefault("action", "")));
        String comment = (String) body.getOrDefault("comment", "");
        checkpointService.adminReview(configId, approved, comment);
        return R.ok(approved ? "审核通过" : "已驳回");
    }

    @PostMapping("/{configId}/sos")
    public R<?> sendSOS(@PathVariable Long configId) {
        R<?> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        checkpointService.sendSOS(sid, configId);
        return R.ok("已通知教师");
    }

    @PostMapping("/{configId}/keywords/followup")
    public R<?> recordFollowup(@PathVariable Long configId, @RequestBody Map<String, Object> body) {
        R<?> disabled = checkEnabled();
        if (disabled != null) return disabled;
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        int keywordIndex = ((Number) body.get("keywordIndex")).intValue();
        boolean correct = Boolean.TRUE.equals(body.get("correct"));
        checkpointService.recordFollowup(sid, configId, keywordIndex, correct);
        return R.ok("已记录");
    }

    @PostMapping("/admin/batch-approve")
    public R<Map<String, Object>> batchApprove(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long subjectId = Long.valueOf(body.get("subjectId").toString());
        int count = checkpointService.adminBatchApprove(subjectId);
        return R.ok(Map.of("approvedCount", count));
    }
}
