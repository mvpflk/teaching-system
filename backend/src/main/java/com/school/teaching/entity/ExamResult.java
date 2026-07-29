package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("backup_exam_results")
public class ExamResult implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;
    private Long studentId;
    private Integer totalScore;
    private Integer scoreEarned;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer blankCount;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private LocalDateTime gradeTime;
    private Integer timeUsedSeconds;
    private Integer cheatWarnings;
    private Integer isPassed;
    private String status;

    private Long schoolId;
    private Long stageId;
    private String scoreType;
    private String gradeLevel;
    private String scoreJson;
    private Integer includeInPortfolio;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
