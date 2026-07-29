package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("classroom_participations")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomParticipation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private Long userId;
    private String participationType;
    private Integer isCorrect;
    private Integer scoreEarned;
    private String response;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String studentName;
}
