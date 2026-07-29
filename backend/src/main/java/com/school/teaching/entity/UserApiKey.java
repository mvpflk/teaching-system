package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_api_keys")
public class UserApiKey {
    private Long id;
    private Long userId;
    private String label;
    private String baseUrl;
    private String encryptedKey;
    private String model;
    private Integer isActive;
    private Integer callCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}