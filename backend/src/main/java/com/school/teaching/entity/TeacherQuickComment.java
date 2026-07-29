package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("teacher_quick_comments")
public class TeacherQuickComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;
    private String commentText;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}