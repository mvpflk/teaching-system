package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("classroom_patrols")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomPatrol implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long teacherId;
    private Long inspectorId;
    private String subject;
    private LocalDate patrolDate;
    private String period;
    private Integer disciplineScore;
    private Integer teachingScore;
    private Integer interactionScore;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
