package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class BuzzSubmitRequest implements Serializable {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
}
