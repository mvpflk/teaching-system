package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long userId;
    private String capability;
    private String provider;
    private String promptHash;
    private Integer tokensUsed;
    private Integer latencyMs;
    private String status;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
