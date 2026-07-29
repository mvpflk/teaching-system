package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class DocumentCompareRequest implements Serializable {
    @NotBlank(message = "模板路径不能为空")
    private String templatePath;

    @NotBlank(message = "学生文件路径不能为空")
    private String studentFilePath;

    private String fileType;
}
