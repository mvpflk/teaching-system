package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class QuizStartRequest implements Serializable {
    @NotNull(message = "班级ID不能为空")
    private Long classId;

    private Long questionId;
    private String questionText;
    private String sceneMode;
    private String questionType;
    private String options;
    private List<Long> excludeStudentIds;
    private Map<String, Object> studentWeights;
}
