package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("typing_levels")
public class TypingLevel implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Integer levelId;
    private Integer exp;
    private String unlockedMaps;
    private LocalDateTime updatedAt;
}
