package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("practice_plans")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PracticePlan implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private String prerequisites;
    private String environment;
    private String safetyNotes;
    private String troubleshooting;
    private String teamRoles;
    private Boolean simpleMode;
    private Boolean shared;
    @TableField("library_type")
    private String libraryType;  // PRIVATE / SHARED / PRESET
    @TableField("tags")
    private String tags;  // JSON
    private String subject;
    private String scoringModel;
    private Long createdBy;
    private String status;
    private String stepsJson;
    private Boolean autoGradingEnabled;
    private String gradingStrategy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
