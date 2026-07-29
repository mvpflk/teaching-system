package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class CreditAdjustRequest implements Serializable {
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    @NotNull(message = "调整金额不能为空")
    private Integer amount;

    private String reason;
}
