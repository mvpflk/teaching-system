package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_settings")
public class SystemSetting implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String settingKey;
    private String settingValue;
    @TableField("default_value")
    private String defaultValue;
    private String valueType;
    private String category;
    private Integer isEditable;
    private String options;
    private String validationRule;
    private String description;
    private Integer orderNum;
    private LocalDateTime updatedAt;
}
