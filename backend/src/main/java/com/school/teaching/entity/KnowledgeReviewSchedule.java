package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_review_schedules")
public class KnowledgeReviewSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long flashcardId;
    private Long nodeId;
    private String sourceType;
    private Long articleId;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer repetitions;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewAt;
    private Integer lastRating;
    private Integer isMastered;
    private LocalDateTime lastPushAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
