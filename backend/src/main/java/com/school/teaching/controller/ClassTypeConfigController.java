package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.ClassTypeConfig;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ClassTypeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class-type-config")
public class ClassTypeConfigController {

    @Autowired private ClassTypeConfigService classTypeConfigService;

    /** 获取班级类型列表（仅普高班/职高班两个选项） */
    @GetMapping("/list")
    public R<List<ClassTypeConfig>> list(@RequestParam(required = false) Long stageId) {
        if (stageId != null) return R.ok(classTypeConfigService.listByStageId(stageId));
        return R.ok(classTypeConfigService.listAll());
    }

    /** 更新类型配置（如修改类型名称或默认专业） */
    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑班级类型配置")
    public R<ClassTypeConfig> update(@PathVariable Long id, @RequestBody ClassTypeConfig config) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(classTypeConfigService.update(id, config));
    }

    // [已禁用] 创建/删除/stats — 班级类型已固化为普高班+职高班两个选项，由数据库种子数据维护
    // 如需新增类型，直接执行SQL INSERT INTO class_type_config
}
