package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_conversations")
public class AgentConversation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String agentType;
    private String title;
    private Integer messageCount;
    private Integer tokenCount;

    /** A-4: 关联的任务ID（create_task 工具创建），用于学习效果闭环 */
    private Long followUpTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
