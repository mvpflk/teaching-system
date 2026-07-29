package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agent_user_memory")
public class AgentUserMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String roleName;
    private String memoryType;   // PREFERENCE / PATTERN / CORRECTION / FACT
    private String memoryKey;
    private String memoryValue;  // JSON string
    private BigDecimal confidence;
    private Integer evidenceCount;
    private LocalDateTime lastEvidenceAt;
    private String sourceSessionId;
    private LocalDateTime expiresAt;
    private String status;       // ACTIVE / SUPERSEDED / EXPIRED
    private Long schoolId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
