package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rectification_notices")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RectificationNotice implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long issueId;
    private String title;
    private String content;
    private Long senderId;
    private Long recipientId;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime compliedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
