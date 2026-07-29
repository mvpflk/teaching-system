package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_role_ext")
public class UserRoleExt implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String scopeType;
    private Long scopeId;
    private String permission;
    private Long grantedBy;
    private LocalDateTime createdAt;
}
