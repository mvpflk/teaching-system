package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class RemoveFromQuizPoolRequest implements Serializable {
    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;
}
