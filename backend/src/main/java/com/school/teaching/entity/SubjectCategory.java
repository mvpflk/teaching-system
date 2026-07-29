package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("subject_category")
public class SubjectCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Long stageId;
    private Long schoolId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
