package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_session_prompt")
@Schema(description = "Agent 对话使用的提示词版本")
public class AgentSessionPrompt implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "学科")
    private String subject;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
