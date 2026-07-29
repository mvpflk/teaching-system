package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class QuickReviewRecordRequest implements Serializable {
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    private Integer cardIndex;
    private Boolean correct;
}
