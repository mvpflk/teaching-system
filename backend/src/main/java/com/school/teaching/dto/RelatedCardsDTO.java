package com.school.teaching.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class RelatedCardsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<WrongNode> wrongKnowledgeNodes;
    private List<CardItem> cards;
    private int totalCards;
    private int estimatedMinutes;

    @Data
    public static class WrongNode implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long nodeId;
        private String nodeName;
        private int wrongCount;
    }

    @Data
    public static class CardItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long cardId;
        private String frontText;
        private String backText;
        private Long articleId;
        private Long knowledgeNodeId;
        private int estimatedMinutes;
    }
}
