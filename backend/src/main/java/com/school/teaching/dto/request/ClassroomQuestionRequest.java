package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.io.Serializable;

@Data
public class ClassroomQuestionRequest implements Serializable {
    private Long taskId;
    private String subject;
    private String chapter;
    private String tag;

    @NotBlank(message = "题目内容不能为空")
    private String content;

    private String referenceAnswer;

    @Min(value = 1, message = "难度等级最小为1")
    @Max(value = 5, message = "难度等级最大为5")
    private Integer difficulty;

    private String source;
    private String questionType;
    private String intent;
    private String aiCategory;
}