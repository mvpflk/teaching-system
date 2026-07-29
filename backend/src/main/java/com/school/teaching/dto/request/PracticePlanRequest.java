package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 实训方案请求。
 * 创建时：title 必填，其余可选。
 * 更新时：title 必填，其余字段可选，仅更新非 null 字段（部分更新）。
 */
@Data
public class PracticePlanRequest implements Serializable {
    /** 方案标题（必填） */
    @NotBlank(message = "方案标题不能为空")
    private String title;

    /** 方案描述（可选） */
    private String description;
    /** 前置条件（可选） */
    private String prerequisites;
    /** 实训环境（可选） */
    private String environment;
    /** 安全须知（可选） */
    private String safetyNotes;
    /** 故障排除（可选） */
    private String troubleshooting;
    /** 团队角色（可选） */
    private String teamRoles;
    /** 评分模式，默认 DUAL_DIMENSION（可选） */
    private String scoringModel;
    /** 评分标准列表（可选） */
    private List<Map<String, Object>> rubrics;
}
