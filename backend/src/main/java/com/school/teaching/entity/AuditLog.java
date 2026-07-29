package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("audit_log")
public class AuditLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String ipAddress;
    private String eventType;
    private String description;
    private String operation;
    private String targetTable;
    private Long targetId;
    private String requestUrl;
    private String method;
    private String params;
    private String oldValue;
    private String newValue;
    private String status;
    private String errorMessage;
    private LocalDateTime createdTime;
}
