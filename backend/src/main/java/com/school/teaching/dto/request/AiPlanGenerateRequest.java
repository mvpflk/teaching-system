package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class AiPlanGenerateRequest implements Serializable {
    @NotBlank(message = "方案标题不能为空")
    private String title;

    private String subject;
    private String requirements;
    private String stageHint;
}
