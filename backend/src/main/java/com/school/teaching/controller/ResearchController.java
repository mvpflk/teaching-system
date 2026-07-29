package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamPaperService;
import com.school.teaching.service.ResearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/research")
public class ResearchController {

    @Autowired private ExamPaperService examPaperService;
    @Autowired private ResearchService researchService;

    // ── P0-1: 试卷标记 ──

    /** 标记为标准化试卷 */
    @PostMapping("/papers/{id}/mark-standardized")
    public R<?> markStandardized(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        String paperRole = (String) body.getOrDefault("paperRole", "COMMON");
        Long parallelPaperId = body.get("parallelPaperId") != null
            ? ((Number) body.get("parallelPaperId")).longValue() : null;
        examPaperService.markStandardized(id, paperRole, parallelPaperId);
        return R.ok("已标记为标准化试卷");
    }

    /** 取消标准化标记 */
    @PostMapping("/papers/{id}/unmark-standardized")
    public R<?> unmarkStandardized(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        examPaperService.unmarkStandardized(id);
        return R.ok("已取消标准化标记");
    }

    /** 锁定试卷 */
    @PostMapping("/papers/{id}/lock")
    public R<?> lockPaper(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        examPaperService.lockPaper(id);
        return R.ok("试卷已锁定");
    }

    /** 解锁试卷（仅管理员） */
    @PostMapping("/papers/{id}/unlock")
    public R<?> unlockPaper(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可解锁");
        examPaperService.unlockPaper(id);
        return R.ok("试卷已解锁");
    }

    // ── P0-2: 基线快照 ──

    /** 拍摄基线快照 */
    @PostMapping("/baseline/capture")
    public R<?> captureBaseline(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        String label = (String) body.getOrDefault("snapshotLabel", "PRETEST");
        try {
            Map<String, Object> result = researchService.captureBaseline(label);
            return R.ok(result, "基线快照拍摄成功");
        } catch (BusinessException e) {
            return R.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("拍摄基线快照失败 label={}: {}", label, e.getMessage(), e);
            return R.error(500, "基线快照拍摄失败，请稍后重试");
        }
    }

    /** 导出基线快照CSV */
    @GetMapping("/baseline/export")
    public ResponseEntity<byte[]> exportBaseline(
            @RequestParam(defaultValue = "PRETEST") String snapshotLabel,
            @RequestParam(required = false) String researchGroup) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        try {
            byte[] csv = researchService.exportBaselineCsv(snapshotLabel, researchGroup);
            String filename = "baseline-" + snapshotLabel.toLowerCase() + ".csv";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode()).build();
        } catch (Exception e) {
            log.error("导出基线CSV失败 label={}: {}", snapshotLabel, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 获取基线快照摘要 */
    @GetMapping("/baseline/summary")
    public R<?> getBaselineSummary(
            @RequestParam(defaultValue = "PRETEST") String snapshotLabel,
            @RequestParam(required = false) String researchGroup) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(researchService.getBaselineSummary(snapshotLabel, researchGroup));
    }

    /** masteryPercent效度验证 — 标准化考试分数 vs 掌握度的Pearson r */
    @GetMapping("/validate-mastery")
    public R<?> validateMastery(
            @RequestParam Long taskId,
            @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        try {
            return R.ok(researchService.validateMastery(taskId, subject));
        } catch (BusinessException e) {
            return R.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("效度验证失败 taskId={}: {}", taskId, e.getMessage(), e);
            return R.error(500, "效度验证失败，请稍后重试");
        }
    }
}
