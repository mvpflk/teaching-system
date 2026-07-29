package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("exam_shares")
public class ExamShare implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String shareCode;
    private Long examId;
    private Long taskId;
    private String taskType;
    private Long creatorId;
    private String creatorName;
    private Long schoolId;
    private String examTitle;
    private String examSubject;
    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private Integer useCount;
}
