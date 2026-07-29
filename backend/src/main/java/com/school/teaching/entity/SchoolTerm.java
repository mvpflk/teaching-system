package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("school_term")
public class SchoolTerm implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isCurrent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
