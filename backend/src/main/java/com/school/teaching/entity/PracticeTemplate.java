package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("practice_templates")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PracticeTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String subject;
    private String category;
    private String stepsJson;
    private String rubricsJson;
    private String scoringModel;
    private String source;
    private Long sourcePlanId;
    private Integer useCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
