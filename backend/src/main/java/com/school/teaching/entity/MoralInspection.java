package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("moral_inspections")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoralInspection implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long inspectorId;
    private LocalDate inspectionDate;
    private String category;
    private Integer score;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
