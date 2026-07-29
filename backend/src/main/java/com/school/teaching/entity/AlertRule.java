package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alert_rules")
public class AlertRule implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String alertType;
    private String taskTypes;
    private Integer minConsecutive;
    private BigDecimal scoreThreshold;
    private Integer daysLookback;
    private Integer cooldownDays;
    private Integer isBuiltin;
    private Integer isEnabled;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
