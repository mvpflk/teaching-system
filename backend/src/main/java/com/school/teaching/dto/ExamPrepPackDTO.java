package com.school.teaching.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ExamPrepPackDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String taskTitle;
    private long daysUntilDeadline;
    private String reason;
    private List<WeakPoint> weakPoints;
    private List<SkipPoint> skipPoints;
    private int totalCards;
    private int totalEstimatedMinutes;

    @Data
    public static class WeakPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long nodeId;
        private String nodeName;
        private BigDecimal masteryPercent;
        private int cardCount;
        private int estimatedMinutes;
    }

    @Data
    public static class SkipPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long nodeId;
        private String nodeName;
        private BigDecimal masteryPercent;
    }
}
