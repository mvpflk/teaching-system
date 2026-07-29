package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 任务题目关联 — 组卷时从题库选题并指定分值、排序。
 */
@Data
@TableName("task_questions")
public class TaskQuestion implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long questionId;

    private String questionType;
    private Integer sortOrder;
    private BigDecimal score;
    private Long schoolId;
    private Long stageId;
    private Long categoryId;
}
