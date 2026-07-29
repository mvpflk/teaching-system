package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("feedbacks")
public class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;          // BUG / SUGGESTION / OTHER
    private String title;
    private String content;
    private String pageUrl;       // 提交时所在页面
    private String browserInfo;   // User-Agent
    private String status;        // OPEN / RESOLVED / CLOSED
    private String adminNote;     // 管理员回复
    private Long resolvedBy;      // 处理人 userId
    private LocalDateTime resolvedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
