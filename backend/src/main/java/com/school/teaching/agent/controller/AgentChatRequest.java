package com.school.teaching.agent.controller;

import lombok.Data;

@Data
public class AgentChatRequest {

    private String agentType;
    private String sessionId;
    private String message;
    private String subject;
}
