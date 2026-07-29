package com.school.teaching.agent.loop;

import lombok.Getter;

@Getter
public enum AgentType {

    LESSON_PREP("备课助手", 0.7, 4096),
    STUDY_BUDDY("学生AI学伴", 0.5, 2048),
    ANALYTICS("学情分析", 0.3, 2048);

    private final String label;
    private final double temperature;
    private final int maxTokens;

    AgentType(String label, double temperature, int maxTokens) {
        this.label = label;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }
}
