package com.school.teaching.agent.session;

import com.school.teaching.agent.loop.AgentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {

    private String id;
    private Long userId;
    private String userRole;
    private AgentType agentType;
    private String title;
    private String summary;
    private List<Message> messages;
    private int tokenCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
