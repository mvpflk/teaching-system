package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("parent_feedback_summaries")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParentFeedbackSummary implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private String period;
    private Integer totalFeedback;
    private Integer positiveCount;
    private Integer negativeCount;
    private String categoriesJson;
    private String summaryText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
