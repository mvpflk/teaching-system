package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("late_submit_requests")
public class LateSubmitRequest implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;
    private String reason;
    private LocalDateTime requestTime;
    private String status;
    private Long handlerId;
    private LocalDateTime handleTime;
    private String handleComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
