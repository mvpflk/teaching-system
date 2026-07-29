package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wrong_questions")
public class WrongQuestion implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long questionId;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private Integer isMastered;
    private String sourceType;
    private Long sourceTaskId;
    private Long sourceSessionId;

    private LocalDateTime lastPracticeTime;

    private Integer practiceCount;

    /** 连续答对次数，达到阈值(默认3)标记掌握 */
    private Integer consecutiveCorrect;
    /** 下次复习计划时间 */
    private LocalDateTime nextReviewAt;
    /** 掌握后连续确认次数，决定复习间隔 */
    private Integer masteredStreak;

    private LocalDateTime masteredAt;

    private String masteredSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
