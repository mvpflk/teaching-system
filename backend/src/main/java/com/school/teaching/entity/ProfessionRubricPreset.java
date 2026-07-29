package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("profession_rubric_preset")
public class ProfessionRubricPreset implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String profession;
    private String wuyuTag;
    private String dimensionsJson;
    private Long schoolId;
    private String scope;
}
