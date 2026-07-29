package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_alert_logs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionAlertLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private String ruleName;
    private String alertMessage;
    private Long targetClassId;
    private Long targetTeacherId;
    private BigDecimal metricValue;
    private BigDecimal threshold;
    private Integer isRead;
    private LocalDateTime triggedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
