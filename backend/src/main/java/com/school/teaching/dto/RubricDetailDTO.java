package com.school.teaching.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RubricDetailDTO implements Serializable {
    private Long rubricId;
    private String rubricName;
    private List<Dimension> dimensions;

    @Data
    public static class Dimension implements Serializable {
        private Long dimensionId;
        private String name;
        private BigDecimal weight;
        private String description;
        private List<Level> levels;
    }

    @Data
    public static class Level implements Serializable {
        private int level;
        private String label;
        private String description;
    }
}
