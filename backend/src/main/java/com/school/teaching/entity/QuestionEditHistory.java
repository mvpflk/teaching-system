package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目编辑历史 — 记录每次版本变更快照
 */
@Data
@TableName("question_edit_history")
public class QuestionEditHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;
    private Integer version;
    private String changeSummary;
    private String beforeSnapshot;
    private String afterSnapshot;
    private Long editedBy;
    private String editType;
    private Long schoolId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
