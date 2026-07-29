package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("group_member")
public class GroupMember implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long teacherId;
    private String groupType;
    private String role;
    @TableField(exist = false)
    private Integer sortOrder;
    @TableField("joined_at")
    private LocalDateTime createTime;
}
