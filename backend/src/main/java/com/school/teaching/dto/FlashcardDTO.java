package com.school.teaching.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class FlashcardDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long cardId;
    private String frontText;
    private String backText;
    private Long articleId;
    private Long knowledgeNodeId;
    private int estimatedMinutes = 2;
}
