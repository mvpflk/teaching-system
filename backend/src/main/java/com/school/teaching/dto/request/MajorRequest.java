package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class MajorRequest implements Serializable {
    @NotBlank(message = "专业名称不能为空")
    private String majorName;
}
