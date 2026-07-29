package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("parent_child_relations")
public class ParentChildRelation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private Long studentId;
    private String relation;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
