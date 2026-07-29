package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("lesson_prep_records")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonPrepRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonPrepGroupId;
    private String title;
    private LocalDate recordDate;
    private String participantIds;
    private Integer participantCount;
    private String content;
    private String outputUrls;
    private Long recordedBy;

    @TableField(exist = false)
    private String groupName;
    @TableField(exist = false)
    private Integer totalMembers;
    @TableField(exist = false)
    private Double participationRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
