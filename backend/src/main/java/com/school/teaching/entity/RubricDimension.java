package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("rubric_dimension")
public class RubricDimension implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rubricId;
    private String name;
    private BigDecimal weight;
    private String description;
    private String levelsJson;
}
