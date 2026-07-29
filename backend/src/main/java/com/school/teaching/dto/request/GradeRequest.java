package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class GradeRequest implements Serializable {
    @NotBlank(message = "年级名称不能为空")
    private String gradeName;
}
