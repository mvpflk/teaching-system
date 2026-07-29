package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("task_templates")
public class TaskTemplate implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String subject;
    private String taskType;
    private String scoreType;
    private String category;         // TEACHING / CLASS_MGMT / SCHOOL_NOTICE
    private String questionIds;      // JSON array
    private String taskConfig;       // JSON object
    private String wuyuTag;
    private Double totalScore;
    private String scope;             // PRIVATE / LESSON_PREP / TEACHING_GROUP / SCHOOL_WIDE / HEAD_TEACHER_GROUP
    private Long teachingGroupId;
    private Long lessonPrepGroupId;
    private Integer useCount;
    private Long createdBy;
    private Long schoolId;
    private Long stageId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private Integer questionCount;
}
