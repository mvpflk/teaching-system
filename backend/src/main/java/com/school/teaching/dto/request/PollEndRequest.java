package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PollEndRequest implements Serializable {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    private List<Integer> manualCounts;
}
