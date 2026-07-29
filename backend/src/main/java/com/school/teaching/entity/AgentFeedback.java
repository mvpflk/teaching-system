package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_feedback")
public class AgentFeedback implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String roleName;
    private Integer messageIndex;
    private Integer rating;
    private String feedbackTags;
    private String comment;
    private String userQuestion;
    private String agentAnswerSnippet;
    private String toolsUsed;
    private Long schoolId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
