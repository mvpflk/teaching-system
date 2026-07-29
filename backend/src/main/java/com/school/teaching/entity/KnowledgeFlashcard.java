package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_flashcards")
public class KnowledgeFlashcard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String frontText;
    private String backText;
    private Integer sortOrder;

    // v167: 卡片质量增强
    private String cardType;                     // DEFINITION/PROCEDURE/COMPARISON/APPLICATION/SCENARIO
    private java.math.BigDecimal qualityScore;   // AI评分 0-100, NULL=未评估
    private String aiComment;                    // AI评语JSON
    private String reviewStatus;                 // PENDING/APPROVED/REJECTED
    private Long linkedQuestionId;               // → question_bank.id
    private String contextPath;                  // 知识点完整路径
    private Long reviewedBy;                     // → users.id
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
