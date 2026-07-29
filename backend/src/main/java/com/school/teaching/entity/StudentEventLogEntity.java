package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("student_event_log")
public class StudentEventLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long schoolId;
    private Long stageId;
    private String eventType;
    private String eventData;
    private String contextJson;
    private String sourceModule;
    private LocalDateTime occurredAt;
}
