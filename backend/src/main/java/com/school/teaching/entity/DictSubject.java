package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dict_subject")
public class DictSubject implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subjectName;
    private Integer sortOrder;
    private Integer status;
    private Integer isPublic;
    /** v169: 卡片Prompt分组 — public-math / public-language / major, NULL=兜底为major */
    private String cardProfileGroup;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
