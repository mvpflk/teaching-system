package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("question_composite_items")
public class QuestionCompositeItems implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentQuestionId;
    private Long childQuestionId;
    private Integer sortOrder;
}
