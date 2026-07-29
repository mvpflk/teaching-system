package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("parent_feedback_responses")
public class ParentFeedbackResponse implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long formId;
    private Long parentId;
    private Long studentId;
    private Integer satisfaction;
    private Integer teachingQuality;
    private Integer homeworkLoad;
    private Integer communication;
    private String comment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
