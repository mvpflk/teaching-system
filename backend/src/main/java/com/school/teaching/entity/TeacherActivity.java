 package com.school.teaching.entity;
 
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.io.Serializable;
 import java.time.LocalDateTime;
 
 @Data
 @TableName("teacher_activity_log")
 public class TeacherActivity implements Serializable {
     @TableId(type = IdType.AUTO)
     private Long id;
     private Long teacherId;
     private String action;
     private String targetType;
     private Long targetId;
     @TableField(fill = FieldFill.INSERT)
     private LocalDateTime createdAt;
 }
