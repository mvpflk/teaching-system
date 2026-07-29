package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("student_favorites")
public class StudentFavorites implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long articleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
