package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("practice_rubrics")
public class PracticeRubric implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String dimension;
    private String dimensionLabel;
    private BigDecimal weight;
    private String criteria;
    private Integer sortOrder;
}
