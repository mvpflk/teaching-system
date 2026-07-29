package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

import java.time.LocalDateTime;

@Data
@TableName("jwt_blacklist")
public class JwtBlacklist implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jti;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
