package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("classroom_sessions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomSession implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Long teacherId;
    private String sessionType;
    private String sceneMode;
    private String status;
    private String questionText;
    private Long questionId;
    private String pollData;
    private Long buzzWinnerId;
    private Long buzzWinnerTime;
    private Long taskId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private String winnerName;
    @TableField(exist = false)
    private Integer participantCount;
}
