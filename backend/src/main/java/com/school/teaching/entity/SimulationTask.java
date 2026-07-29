package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("simulation_tasks")
public class SimulationTask implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long nodeId;
    private String taskJson;
    private String initialVfs;
    private String mode;
    private String category;
    private Integer difficulty;
    private Integer timeLimit;
    private Long createdBy;
    private Long schoolId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
