package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AlertService;
import com.school.teaching.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    @Autowired private ParentService parentService;
    @Autowired private AlertService alertService;

    /** 家长查看自己关联的所有孩子 */
    @GetMapping("/children")
    public R<List<Map<String, Object>>> myChildren() {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(parentService.getMyChildren(parentUserId));
    }

    /** 查看指定孩子的成绩（最近10条） */
    @GetMapping("/children/{studentId}/grades")
    public R<List<Map<String, Object>>> childGrades(@PathVariable Long studentId) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(parentService.getChildGrades(parentUserId, studentId));
    }

    /** 查看指定孩子的成长足迹（最近20条） */
    @GetMapping("/children/{studentId}/timeline")
    public R<List<Map<String, Object>>> childTimeline(@PathVariable Long studentId) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(parentService.getChildTimeline(parentUserId, studentId));
    }

    /** 获取关联孩子的预警列表 */
    @GetMapping("/alerts")
    public R<List<Map<String, Object>>> childAlerts() {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(alertService.getChildAlerts(parentUserId));
    }

    /** 获取关联孩子的未读预警数 */
    @GetMapping("/alerts/unread-count")
    public R<Map<String, Object>> unreadAlertCount() {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(Map.of("count", alertService.getChildUnreadAlertCount(parentUserId)));
    }

    /** 家长通过学号+学生姓名自助绑定孩子 */
    @PostMapping("/bind")
    public R<Map<String, Object>> bindChild(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        String studentNumber = (String) body.get("studentNumber");
        String studentName = (String) body.get("studentName");
        String relation = (String) body.get("relation");
        return R.ok(parentService.bindChild(parentUserId, studentNumber, studentName, relation));
    }

    /** 查看指定孩子的作业列表（最近30天） */
    @GetMapping("/children/{studentId}/homework")
    public R<List<Map<String, Object>>> childHomework(@PathVariable Long studentId) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(parentService.getChildHomework(parentUserId, studentId));
    }

    /** 家长确认已读某条预警 */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public R<Void> acknowledgeAlert(@PathVariable Long alertId) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        parentService.acknowledgeAlert(parentUserId, alertId);
        return R.ok(null);
    }

    /** 查看指定孩子的实训记录 */
    @GetMapping("/child-practices/{studentId}")
    public R<List<Map<String, Object>>> childPractices(@PathVariable Long studentId) {
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin()) return R.error(403, "仅家长/管理员可访问");
        Long parentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(parentService.getChildPractices(parentUserId, studentId));
    }
}