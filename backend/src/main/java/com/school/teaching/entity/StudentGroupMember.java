package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("group_members")
public class StudentGroupMember implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long studentId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
