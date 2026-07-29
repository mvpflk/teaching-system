package com.school.teaching.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rubric_scores")
public class RubricScore implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long rubricId;
    private Long dimensionId;
    private Integer level;
    private BigDecimal score;
    private String comment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
