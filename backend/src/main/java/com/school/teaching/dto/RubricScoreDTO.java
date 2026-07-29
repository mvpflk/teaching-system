package com.school.teaching.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RubricScoreDTO implements Serializable {
    private Long rubricId;
    private List<DimensionScore> dimensions;
    private BigDecimal totalScore;

    @Data
    public static class DimensionScore implements Serializable {
        private Long dimensionId;
        private String dimensionName;
        private BigDecimal weight;
        private Integer level;
        private String levelLabel;
        private BigDecimal score;
        private String comment;
    }
}
