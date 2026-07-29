package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 任务评分请求。
 */
@Data
public class TaskGradeRequest implements Serializable {
    /** 提交ID（必填） */
    @NotNull(message = "提交ID不能为空")
    private Long submissionId;

    /** 分数（可选，PASS_FAIL模式可不传） */
    private Object score;

    /** 评分等级（如 A/B/C 或 PASS/FAIL） */
    private String gradeLevel;

    /** 评语（可选） */
    private String comment;

    /** 评分解释（可选） */
    private String explanation;

    /** 是否为优秀范例（可选） */
    private Integer isExemplar;

    /** 分数类型：SCORE 或 PASS_FAIL */
    private String scoreType;
}
