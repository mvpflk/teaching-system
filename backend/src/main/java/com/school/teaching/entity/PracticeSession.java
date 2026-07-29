package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("practice_sessions")
public class PracticeSession implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String sourceType;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer wrongCount;
    private String status;
    private String weakPoints;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
