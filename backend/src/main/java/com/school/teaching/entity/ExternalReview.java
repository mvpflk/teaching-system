package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("external_review")
public class ExternalReview implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submissionId;
    private String token;
    private String reviewerName;
    private String status;
    private String scoreJson;
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;
}
