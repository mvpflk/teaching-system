package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("checkpoint_memory_card")
public class CheckpointMemoryCard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long configId;
    private String cardJson;
    private LocalDateTime lastReviewedAt;
    private Integer reviewCount;
    private LocalDateTime lastPushAt;
    private LocalDateTime createdAt;
}
