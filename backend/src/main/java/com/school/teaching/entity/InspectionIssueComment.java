package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("inspection_issue_comments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionIssueComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long issueId;
    private Long userId;
    private String content;
    private Integer isSystem;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
