package com.school.teaching.agent.controller;

import com.school.teaching.agent.prompt.PromptTemplateCache;
import com.school.teaching.agent.prompt.PromptTemplateService;
import com.school.teaching.common.R;
import com.school.teaching.entity.PromptTemplate;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent/prompts")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService service;
    @Autowired
    private PromptTemplateCache cache;

    @GetMapping
    @Operation(summary = "列出模板（按 type 筛选，默认 TEMPLATE）")
    @PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
    public R<List<PromptTemplate>> list(@RequestParam(defaultValue = "TEMPLATE") String type) {
        return R.ok(service.listByType(type));
    }

    @GetMapping("/{name}/versions")
    @Operation(summary = "查看某个模板的所有版本")
    @PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
    public R<List<PromptTemplate>> listVersions(@PathVariable String name) {
        return R.ok(service.listVersions(name));
    }

    @GetMapping("/{name}/versions/{versionId}")
    @Operation(summary = "查看某个版本的详情")
    @PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
    public R<PromptTemplate> getVersion(@PathVariable Long versionId) {
        return R.ok(service.getById(versionId));
    }

    @PostMapping("/{name}/versions")
    @Operation(summary = "新增版本")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public R<PromptTemplate> createVersion(@PathVariable String name, @RequestBody PromptTemplate template) {
        template.setName(name);
        if (template.getType() == null) template.setType("TEMPLATE");
        return R.ok(service.createVersion(template));
    }

    @PutMapping("/{name}/activate/{versionId}")
    @Operation(summary = "激活指定版本")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public R<Void> activateVersion(@PathVariable String name, @PathVariable Long versionId) {
        service.activateVersion(name, versionId);
        return R.ok();
    }

    @GetMapping("/{name}/diff")
    @Operation(summary = "对比两个版本差异")
    @PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
    public R<String> diff(@RequestParam Long v1, @RequestParam Long v2) {
        PromptTemplate a = service.getById(v1);
        PromptTemplate b = service.getById(v2);
        if (a == null || b == null) return R.error(400, "版本不存在");
        String diff = "--- v" + a.getVersion() + " (" + a.getId() + ")\n" + a.getContent()
                    + "\n\n+++ v" + b.getVersion() + " (" + b.getId() + ")\n" + b.getContent();
        return R.ok(diff);
    }

    @PostMapping
    @Operation(summary = "创建/更新 FINAL 覆盖（name+subject 已存在则覆盖内容）")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public R<PromptTemplate> upsertFinal(@RequestBody PromptTemplate template) {
        template.setType("FINAL");
        return R.ok(service.upsertFinal(template));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板/覆盖")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/refresh")
    @Operation(summary = "手动刷新缓存")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public R<Void> refreshCache() {
        cache.refresh();
        return R.ok();
    }
}