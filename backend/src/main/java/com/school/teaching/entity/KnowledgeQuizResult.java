package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_quiz_results")
public class KnowledgeQuizResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long articleId;
    private Long subjectId;
    private Integer totalQuestions;
    private Integer correctCount;
    private BigDecimal score;
    private String wrongQuestionIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
