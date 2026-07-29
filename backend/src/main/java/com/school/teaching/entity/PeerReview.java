package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("peer_review")
public class PeerReview implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long reviewerId;
    private Long submissionId;
    private String scoreJson;
    private LocalDateTime submittedAt;
}
