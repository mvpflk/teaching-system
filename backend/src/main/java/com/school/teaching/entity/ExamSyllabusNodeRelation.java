package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("exam_syllabus_node_relation")
public class ExamSyllabusNodeRelation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long syllabusId;
    private Long nodeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
