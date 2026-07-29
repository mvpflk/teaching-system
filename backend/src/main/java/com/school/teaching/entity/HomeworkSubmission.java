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
@TableName("backup_homework_submissions")
public class HomeworkSubmission implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeworkId;
    private Long studentId;
    private String content;
    private String contentType;
    private String attachmentUrl;
    private String attachmentNames;
    private LocalDateTime submitTime;
    private Integer isLate;
    private Integer lateHours;
    private Integer score;
    private String scoreStatus;
    private String teacherComment;
    private LocalDateTime gradedTime;
    private Long gradedBy;

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

    // Non-database fields
    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String homeworkTitle;

    @TableField(exist = false)
    private LocalDateTime endTime;

    // 等级制评分的等级 (A+, A, B+, B)
    @TableField(exist = false)
    private String grade;

    // 该次批改获得的积分 (前端显示用)
    @TableField(exist = false)
    private Integer creditAwarded;
}
