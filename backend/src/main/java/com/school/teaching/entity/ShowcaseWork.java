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
@TableName("showcase_works")
public class ShowcaseWork implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String sourceType;
    private Long sourceId;
    private Long studentId;
    private Long classId;
    private String subject;
    private Long teacherId;
    private String teacherComment;
    private String showScope;
    private String targetClassIds;
    private Integer creditAwarded;
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String grade;
    @TableField(exist = false)
    private String submissionContent;
    @TableField(exist = false)
    private String submissionScore;
    @TableField(exist = false)
    private String submissionAttachments;
    @TableField(exist = false)
    private String firstImageUrl;
}
