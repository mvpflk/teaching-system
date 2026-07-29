package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("practice_session_items")
public class PracticeSessionItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long questionId;
    private String questionType;
    private String questionText;
    private String options;
    private String correctAnswer;
    private String explanation;
    private String source;
    private String studentAnswer;
    private Integer isCorrect;
    private java.math.BigDecimal autoScore;
    private LocalDateTime answeredAt;
    private Integer sortOrder;
}
