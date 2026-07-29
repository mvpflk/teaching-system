package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class PollVoteRequest implements Serializable {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotNull(message = "选项索引不能为空")
    private Integer optionIndex;
}
