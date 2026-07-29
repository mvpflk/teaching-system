package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class PlanBatchImportRequest implements Serializable {
    private String markdown;
}
