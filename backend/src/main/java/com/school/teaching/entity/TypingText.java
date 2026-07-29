package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("typing_texts")
public class TypingText implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String language;
    private Integer difficulty;
    private String category;
    private String type;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
