package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("edu_stage_config")
public class EduStageConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private String capabilityKey;
    private Integer enabled;
    private String configJson;
}
