package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("practice_step_grades")
public class PracticeStepGrade implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long stepId;
    private BigDecimal stepScore;
    private String stepComment;
}
