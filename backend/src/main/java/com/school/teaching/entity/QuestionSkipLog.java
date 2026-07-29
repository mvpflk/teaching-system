package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("question_skip_log")
public class QuestionSkipLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long studentId;
    private String reason;  // TOO_HARD / OUT_OF_SYLLABUS / UNCLEAR
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
