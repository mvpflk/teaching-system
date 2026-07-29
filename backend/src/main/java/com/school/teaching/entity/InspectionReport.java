package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inspection_reports")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionReport implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String reportType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private String summaryJson;
    private Integer issueCount;
    private Integer resolvedCount;
    private Integer noticeCount;
    private String status;
    private Long generatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
