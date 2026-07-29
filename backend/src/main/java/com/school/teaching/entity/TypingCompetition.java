package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("typing_competitions")
public class TypingCompetition implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long textId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField("allowed_class_ids")
    private String allowedClassIds;
    private Integer durationMinutes;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String textTitle;
    @TableField(exist = false)
    private String textContent;
}
