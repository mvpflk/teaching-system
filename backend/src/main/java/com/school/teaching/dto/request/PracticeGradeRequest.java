package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PracticeGradeRequest implements Serializable {
    @NotNull(message = "提交ID不能为空")
    private Long submissionId;

    private BigDecimal overallScore;
    private String overallComment;
    private List<Map<String, Object>> stepGrades;
}
