package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("checkpoint_config")
public class CheckpointConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;
    private String chapterName;
    private String taskName;
    private Long taskNodeId;
    private Integer seq;

    private String keyPointsJson;

    private String questionSource;
    private Integer questionCount;

    private Integer difficultyLevel;

    private Integer practiceCount;

    private String checkpointType;
    private String parentConfigId;

    private String reviewStatus;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
