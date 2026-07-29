package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("practice_step_images")
public class PracticeStepImage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stepId;
    private String imageUrl;
    private Integer orderIndex;
}
