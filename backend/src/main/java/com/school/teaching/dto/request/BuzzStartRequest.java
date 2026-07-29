package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class BuzzStartRequest implements Serializable {
    @NotNull(message = "班级ID不能为空")
    private Long classId;

    private String questionText;
    private Integer scoreReward;
}
