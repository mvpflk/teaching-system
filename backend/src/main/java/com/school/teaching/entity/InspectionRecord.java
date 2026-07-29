package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inspection_records")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inspectorId;
    private String recordType;
    private String title;
    private String description;
    private String location;
    private Long targetClassId;
    private Long targetTeacherId;
    private String severity;
    private String status;
    private String attachmentUrls;
    private LocalDate recordDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
