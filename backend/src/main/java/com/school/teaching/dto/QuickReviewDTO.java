package com.school.teaching.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class QuickReviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<CardItem> cards;
    private int totalCards;

    @Data
    public static class CardItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private int index;
        private Long cardId;
        private String frontText;
        private String backText;
        private Long articleId;
    }
}
