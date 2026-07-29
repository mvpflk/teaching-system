package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rubric")
public class Rubric implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long schoolId;
    private Long stageId;
    private Long createdBy;
    private String scope;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
