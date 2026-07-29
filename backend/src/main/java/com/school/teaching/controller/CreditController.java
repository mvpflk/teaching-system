package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.entity.CreditShopItem;
import com.school.teaching.entity.Student;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/credit")
@Tag(name = "积分体系", description = "积分查询、签到、商城兑换、称号管理")
public class CreditController {

    @Autowired private CreditService creditService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private StudentService studentService;
    @Autowired private com.school.teaching.service.SystemService systemService;

    /** 积分系统开关检查 */
    private boolean creditEnabled() {
        return systemService.getBooleanConfig("feature.credit_enabled", true);
    }

    @Operation(summary = "获取积分信息", description = "学生获取个人积分余额和等级信息")
    @GetMapping("/actions/info")
    public R<Map<String, Object>> getCreditInfo() {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(creditService.getCreditInfo(sid));
    }

    @Operation(summary = "获取积分流水", description = "获取积分变动记录")
    @Parameter(name = "studentId", description = "学生ID（教师查看他人时传）")
    @GetMapping("/actions/transactions")
    public R<?> getTransactions(@RequestParam(required = false) Long studentId) {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        if (studentId != null && SecurityUtils.isTeacherOrAdmin()) {
            return R.ok(creditService.getTransactions(studentId));
        }
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(creditService.getTransactions(sid));
    }

    @GetMapping("/actions/ranking")
    @Operation(summary = "获取积分排行榜", description = "获取积分排行榜数据")
    @Parameter(name = "type", description = "排行类型", example = "total")
    @Parameter(name = "limit", description = "返回数量", example = "100")
    @Parameter(name = "classId", description = "班级ID")
    @Parameter(name = "grade", description = "年级")
    @Parameter(name = "major", description = "专业")
    public R<?> getRanking(@RequestParam(defaultValue = "total") String type,
                            @RequestParam(defaultValue = "100") int limit,
                            @RequestParam(required = false) Long classId,
                            @RequestParam(required = false) String grade,
                            @RequestParam(required = false) String major) {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        return R.ok(creditService.getRanking(type, limit, classId, grade, major));
    }

    @GetMapping("/actions/moral-ranking")
    @Operation(summary = "获取德育积分排行", description = "获取德育积分排行榜")
    @Parameter(name = "classId", description = "班级ID")
    @Parameter(name = "grade", description = "年级")
    @Parameter(name = "limit", description = "返回数量", example = "50")
    public R<?> getMoralRanking(@RequestParam(required = false) Long classId,
                                 @RequestParam(required = false) String grade,
                                 @RequestParam(defaultValue = "50") int limit) {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        return R.ok(creditService.getMoralRanking(classId, grade, limit));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "签到")
    @Operation(summary = "每日签到", description = "学生每日签到获取积分")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "签到成功"),
        @ApiResponse(responseCode = "404", description = "未找到学生信息")
    })
    @PostMapping("/actions/sign")
    public R<Map<String, Object>> signIn() {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(creditService.signIn(sid));
    }

    @Operation(summary = "获取商城商品", description = "获取积分商城可兑换商品列表")
    @GetMapping("/shop")
    public R<?> getShopItems() {
        if (!creditEnabled() || !systemService.getBooleanConfig("feature.shop_enabled", true)) return R.error(503, "积分商城暂未开放");
        return R.ok(creditService.getShopItems());
    }

    @Operation(summary = "获取商品详情", description = "获取指定商品的详细信息")
    @Parameter(name = "id", description = "商品ID", required = true, example = "123")
    @GetMapping("/shop/{id}")
    public R<?> getShopItem(@PathVariable Long id) {
        if (!creditEnabled() || !systemService.getBooleanConfig("feature.shop_enabled", true)) return R.error(503, "积分商城暂未开放");
        var items = creditService.getShopItems();
        var item = items.stream().filter(i -> i.getId().equals(id)).findFirst();
        return item.isPresent() ? R.ok(item.get()) : R.notFound("商品不存在");
    }

    @AuditLog(eventType = AuditEventType.CREDIT_REDEEM, description = "积分兑换")
    @Operation(summary = "积分兑换", description = "使用积分兑换商城商品")
    @PostMapping("/actions/redeem")
    public R<Map<String, Object>> redeem(@Valid @RequestBody CreditRedeemRequest request) {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(creditService.redeemItem(sid, request.getItemId()));
    }

    @Operation(summary = "获取称号等级", description = "获取积分系统的称号等级列表")
    @GetMapping("/titles")
    public R<?> getTitles() { if (!creditEnabled()) return R.error(503, "积分系统暂未开放"); return R.ok(creditService.getTitleLevels()); }

    @Operation(summary = "获取成就", description = "学生获取个人成就列表")
    @GetMapping("/actions/achievements")
    public R<?> getAchievements() {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(creditService.getAchievements(sid));
    }

    // ===== Admin endpoints =====

    @Operation(summary = "管理端学生列表", description = "管理员查看积分相关的学生列表")
    @Parameter(name = "keyword", description = "搜索关键词")
    @GetMapping("/admin/students")
    public R<?> adminListStudents(@RequestParam(required = false) String keyword) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(creditService.listStudents(keyword));
    }

    @AuditLog(eventType = AuditEventType.CREDIT_ADJUST, description = "积分调整")
    @Operation(summary = "调整积分", description = "教师/管理员调整学生积分")
    @PostMapping("/actions/adjust-credit")
    public R<Map<String, Object>> adjustCredit(@Valid @RequestBody CreditAdjustRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        String reason = request.getReason() != null ? request.getReason() : "";
        return R.ok(creditService.adjustCredit(request.getStudentId(), request.getAmount(), reason));
    }

    @Operation(summary = "创建商城商品", description = "管理员创建积分商城商品")
    @PostMapping("/shop")
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建商城商品")
    public R<?> createShopItem(@Valid @RequestBody ShopItemRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        var item = new com.school.teaching.entity.CreditShopItem();
        item.setItemName(request.getItemName());
        item.setCreditPrice(request.getCreditPrice());
        item.setDescription(request.getDescription());
        return R.ok(creditService.createShopItem(item));
    }

    @Operation(summary = "更新商城商品", description = "管理员更新积分商城商品")
    @Parameter(name = "id", description = "商品ID", required = true, example = "123")
    @PutMapping("/shop/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新商城商品")
    public R<?> updateShopItem(@PathVariable Long id, @RequestBody ShopItemRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        var item = new com.school.teaching.entity.CreditShopItem();
        item.setId(id);
        if (request.getItemName() != null) item.setItemName(request.getItemName());
        if (request.getCreditPrice() != null) item.setCreditPrice(request.getCreditPrice());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        return R.ok(creditService.updateShopItem(item));
    }

    @Operation(summary = "删除商城商品", description = "管理员删除积分商城商品")
    @Parameter(name = "id", description = "商品ID", required = true, example = "123")
    @DeleteMapping("/shop/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除商城商品")
    public R<String> deleteShopItem(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        creditService.deleteShopItem(id);
        return R.ok("已删除");
    }

    @Operation(summary = "获取积分规则", description = "管理员获取积分规则列表")
    @GetMapping("/admin/rules")
    public R<?> getRules() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(creditService.getRules());
    }

    @Operation(summary = "创建积分规则", description = "管理员创建积分获取规则")
    @PostMapping("/admin/rules")
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建积分规则")
    public R<?> createRule(@Valid @RequestBody CreditRuleRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        var rule = new com.school.teaching.entity.CreditRule();
        rule.setRuleCode(request.getRuleCode());
        rule.setCreditValue(request.getCreditValue());
        return R.ok(creditService.createRule(rule));
    }

    @Operation(summary = "更新积分规则", description = "管理员更新积分规则")
    @Parameter(name = "id", description = "规则ID", required = true, example = "123")
    @PutMapping("/admin/rules/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新积分规则")
    public R<?> updateRule(@PathVariable Long id, @RequestBody CreditRuleRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        var rule = new com.school.teaching.entity.CreditRule();
        rule.setId(id);
        if (request.getCreditValue() != null) rule.setCreditValue(request.getCreditValue());
        return R.ok(creditService.updateRule(rule));
    }

    @Operation(summary = "删除积分规则", description = "管理员删除积分规则")
    @Parameter(name = "id", description = "规则ID", required = true, example = "123")
    @DeleteMapping("/admin/rules/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除积分规则")
    public R<String> deleteRule(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        creditService.deleteRule(id);
        return R.ok("已删除");
    }

    @Operation(summary = "获取交付列表", description = "管理员查看待交付的兑换记录")
    @Parameter(name = "status", description = "状态筛选")
    @GetMapping("/admin/deliveries")
    public R<?> getDeliveries(@RequestParam(required = false) String status) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(creditService.getDeliveries(status));
    }

    @Operation(summary = "标记已交付", description = "管理员标记兑换商品已交付")
    @Parameter(name = "id", description = "交付ID", required = true, example = "123")
    @PutMapping("/admin/deliveries/{id}/actions/deliver")
    @AuditLog(eventType = AuditEventType.OTHER, description = "标记发货")
    public R<String> markDelivered(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        creditService.markDelivered(id);
        return R.ok("已标记为已交付");
    }

    @Operation(summary = "获取自定义称号", description = "获取学生当前的自定义称号")
    @GetMapping("/actions/custom-title")
    public R<Map<String, Object>> getCustomTitle() {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.ok(Map.of("valid", false, "customTitle", ""));
        Student s = studentService.getById(sid);
        if (s == null) return R.ok(Map.of("valid", false, "customTitle", ""));
        String ct = s.getCustomTitle();
        boolean valid = ct != null && s.getCustomTitleSetAt() != null
            && java.time.LocalDateTime.now().isBefore(s.getCustomTitleSetAt().plusDays(7));
        return R.ok(Map.of("valid", valid, "customTitle", valid ? ct : ""));
    }

    /** 学生自己设置自定义称号（兑换后） */
    @Operation(summary = "设置自定义称号", description = "学生设置自己的自定义称号")
    @PutMapping("/actions/custom-title")
    public R<String> setMyCustomTitle(@RequestBody CustomTitleRequest request) {
        if (!creditEnabled()) return R.error(503, "积分系统暂未开放");
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        creditService.setCustomTitle(sid, request.getTitleCode());
        return R.ok("已设置称号");
    }

    @Operation(summary = "管理员设置自定义称号", description = "管理员为学生设置自定义称号")
    @Parameter(name = "studentId", description = "学生ID", required = true, example = "456")
    @PutMapping("/admin/students/{studentId}/actions/custom-title")
    @AuditLog(eventType = AuditEventType.OTHER, description = "设置自定义称号")
    public R<String> setCustomTitle(@PathVariable Long studentId, @RequestBody CustomTitleRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        creditService.setCustomTitle(studentId, request.getTitleCode());
        return R.ok("已设置称号");
    }

    // ── 管理端别名 ──
    @Operation(summary = "管理端商品列表", description = "管理员查看商城商品列表")
    @GetMapping("/admin/shop")  public R<?> adminShop()  { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); return R.ok(creditService.getShopItems()); }
    @Operation(summary = "管理端创建商品", description = "管理员创建商城商品")
    @AuditLog(eventType = AuditEventType.OTHER, description = "管理员创建商品")
    @PostMapping("/admin/shop") public R<?> adminCreateShop(@RequestBody CreditShopItem item) { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); creditService.createShopItem(item); return R.ok(item); }
    @Operation(summary = "管理端更新商品", description = "管理员更新商城商品")
    @AuditLog(eventType = AuditEventType.OTHER, description = "管理员更新商品")
    @PutMapping("/admin/shop/{id}") public R<?> adminUpdateShop(@PathVariable Long id, @RequestBody CreditShopItem item) { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); item.setId(id); creditService.updateShopItem(item); return R.ok(); }
    @Operation(summary = "管理端删除商品", description = "管理员删除商城商品")
    @AuditLog(eventType = AuditEventType.OTHER, description = "管理员删除商品")
    @DeleteMapping("/admin/shop/{id}") public R<?> adminDeleteShop(@PathVariable Long id) { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); creditService.deleteShopItem(id); return R.ok(); }
    @Operation(summary = "管理端称号列表", description = "管理员查看称号等级列表")
    @GetMapping("/admin/titles") public R<?> adminTitles() { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); return R.ok(creditService.getTitleLevels()); }
    @Operation(summary = "管理端更新称号", description = "管理员更新称号等级")
    @AuditLog(eventType = AuditEventType.OTHER, description = "管理称号等级")
    @PutMapping("/admin/titles/{id}") public R<?> adminUpdateTitle(@PathVariable Long id, @RequestBody Map<String,Object> b) { if (!SecurityUtils.isAdmin()) return R.error(403, "无权限"); return R.ok(); }
}
