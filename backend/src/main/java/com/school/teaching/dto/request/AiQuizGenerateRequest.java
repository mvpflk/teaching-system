package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class AiQuizGenerateRequest implements Serializable {
    private String subject;
    private String knowledgePoint;
    private Integer count;
    private String stageHint;
    private Long nodeId;
    private String teachingGoal;
    private Integer difficulty;
    private String questionType;
}
