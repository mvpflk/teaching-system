package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("teacher_reference_questions")
public class TeacherReferenceQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String subject;

    private String questionType;

    private String contentJson;

    private String source;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
