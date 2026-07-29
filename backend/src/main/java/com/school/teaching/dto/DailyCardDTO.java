package com.school.teaching.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class DailyCardDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long cardId;
    private String frontText;
    private String backText;
    private Long articleId;
    private Long knowledgeNodeId;
    private String knowledgeNodeName;
    private String subjectName;
    private String reason;
    private String cardType;        // v167: DEFINITION/PROCEDURE/COMPARISON/APPLICATION/SCENARIO
    private Integer todayReviewed;
    private Integer dailyGoal = 5;
    private Integer streakDays;
}
