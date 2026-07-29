package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class BuzzGradeRequest implements Serializable {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    @NotNull(message = "结果不能为空")
    private Integer result;

    private String response;
}
