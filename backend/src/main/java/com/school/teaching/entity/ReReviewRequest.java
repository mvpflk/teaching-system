package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("re_review_request")
public class ReReviewRequest implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long studentId;
    private String reason;
    private String status;
    private String teacherComment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
