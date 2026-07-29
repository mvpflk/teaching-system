package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("checkpoint_progress")
public class CheckpointProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long configId;
    private Long subjectId;

    private Integer keywordsPassed;
    private Integer keywordsSkipped;
    private Integer keywordsAttempts;

    private Integer checkpointPassed;
    private LocalDateTime passedAt;
    private Integer attempts;
    private Integer correctCount;

    private Integer creditGranted;
    private LocalDateTime creditGrantedAt;
    private Integer creditAmount;

    private Integer lastNodeIndex;

    private String wrongQuestionIds;
    private String questionIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
