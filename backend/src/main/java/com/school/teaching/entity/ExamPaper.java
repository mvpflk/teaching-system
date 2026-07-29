package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_papers")
public class ExamPaper implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subject;
    private String questionIds;     // JSON
    private Integer questionCount;
    private String typeStats;       // JSON
    private String scorePresets;    // JSON
    private BigDecimal totalScore;
    private String examConfig;      // JSON
    private Integer durationMinutes;
    private String sourceFile;
    private String contentHash;
    private Long creatorId;
    private Integer status;
    private Integer useCount;
    private Long lastTaskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long schoolId;

    // P0-1: 标准化试卷标记
    private Integer isStandardized;
    private String paperRole;        // PRETEST/POSTTEST/MIDTEST/COMMON
    private Long parallelPaperId;    // 平行卷关联
    private LocalDateTime lockedAt;
    private Long lockedBy;
}
