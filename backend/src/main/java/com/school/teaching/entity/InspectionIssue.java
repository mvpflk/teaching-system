package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inspection_issues")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionIssue implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String status;
    private Long assignedTo;
    private Long assignedClassId;
    private Long relatedTaskId;
    private LocalDate deadline;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String resolveComment;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String verifyComment;
    private String attachmentUrls;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
