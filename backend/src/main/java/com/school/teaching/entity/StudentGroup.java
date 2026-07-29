package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("student_groups")
public class StudentGroup implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private String name;
    private Integer sortOrder;
    private Long subjectId;
    private String groupType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
