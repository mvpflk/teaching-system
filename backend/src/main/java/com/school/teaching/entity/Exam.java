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
@TableName("backup_exams")
public class Exam implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private Long classId;
    private Long teacherId;
    private String subject;
    private String examType;
    private Integer totalScore;
    private Integer passingScore;
    private Integer questionCount;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isRandomOrder;
    private Integer isShowAnswer;
    private Integer allowCheatDetection;
    private Integer maxCheatWarnings;
    private String status;

    // 多租户
    private Long schoolId;
    private Long stageId;

    // 统一任务
    private String category;
    private String taskConfig;
    private String targetType;
    private Long targetId;
    private Integer notifyParents;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
