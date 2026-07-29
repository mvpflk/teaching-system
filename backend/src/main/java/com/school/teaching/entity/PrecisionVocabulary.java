package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("precision_vocabulary")
public class PrecisionVocabulary implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String word;
    private Integer masterLevel;
    private LocalDateTime lastReviewAt;
    private LocalDateTime nextReviewAt;
    private Integer correctCount;
    private Integer wrongCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
