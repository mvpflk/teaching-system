package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class QuickReviewStartRequest implements Serializable {
    private Integer limit;
    private Long subjectId;
    private Long nodeId;
}
