package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("precision_progress")
public class PrecisionProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String subject;
    private Long nodeId;
    private BigDecimal masteryPercent;
    private Integer totalAttempts;
    private Integer totalCorrect;
    private Integer consecutiveCorrect;
    private String stepProgress;
    private LocalDateTime lastPracticeAt;
    private LocalDateTime nextReviewAt;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
