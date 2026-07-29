package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PollStartRequest implements Serializable {
    @NotNull(message = "班级ID不能为空")
    private Long classId;

    private String questionText;
    private List<String> options;
    private Integer durationSeconds;
    private Boolean anonymous;
}
