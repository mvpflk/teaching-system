package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 切屏事件审计日志 — 每次切屏记录一条，用于教师端证据查看和申诉处理
 */
@Data
@TableName("cheat_event_log")
public class CheatEventLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long studentId;
    private Long submissionId;

    /** 事件类型: VISIBILITY_HIDDEN / FULLSCREEN_EXIT / UNKNOWN */
    private String eventType;

    /** 触发时的累计切屏次数 */
    private Integer cheatWarnings;

    /** 考试配置的切屏上限 */
    private Integer maxWarnings;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
