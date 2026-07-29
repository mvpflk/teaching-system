package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("parent_feedback_forms")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParentFeedbackForm implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private String title;
    private String period;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime closedAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
