package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_alert_rules")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionAlertRule implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;
    private String ruleType;
    private String targetType;
    private BigDecimal threshold;
    private String comparison;
    private String timeWindow;
    private Integer enabled;
    private Integer notifyInspector;
    private Integer notifyTeacher;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
