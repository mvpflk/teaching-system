package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("simulation_recordings")
public class SimulationRecording implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long studentId;
    private String eventsJson;
    private Integer eventCount;
    private Integer durationSeconds;
    private Integer success;
    private BigDecimal autoScore;
    private String teacherNotes;
    private Long schoolId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
